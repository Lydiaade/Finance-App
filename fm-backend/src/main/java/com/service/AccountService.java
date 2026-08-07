package com.service;

import com.dto.BankAccount;
import com.dto.MonthlyTransactionTotal;
import com.dto.Transaction;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FinanceManagerService financeManagerService;

    public List<BankAccount> getAllAccounts() {
        return accountRepository.findAll();
    }

    public BankAccount getAccount(int id) throws FileNotFoundException {
        Optional<BankAccount> account = accountRepository.findById(id);
        if (account.isEmpty()) {
            throw new FileNotFoundException("This account does not exist");
        }
        return account.get();
    }

    public List<Transaction> getAccountTransactions(int id) {
        return transactionRepository.findAllByAccount_Id(id);
    }

    public List<MonthlyTransactionTotal> getAccountAnnualMonthlyTransactions(int id) {
        LocalDate yearPriorToToday = LocalDate.now().minusYears(1);
        System.out.println(yearPriorToToday);
        List<MonthlyTransactionTotal> annualMonthlyTransactions = new ArrayList<>();
        while (yearPriorToToday.equals(LocalDate.now()) || yearPriorToToday.isBefore(LocalDate.now())) {
            String currentMonthYear = yearPriorToToday.getMonth().getDisplayName(TextStyle.SHORT, Locale.US) + " " + yearPriorToToday.getYear();
            List<Transaction> transactions = transactionRepository.findAllByAccount_IdAndDateInMonthYear(
                    id, yearPriorToToday.getMonthValue(), yearPriorToToday.getYear());
            BigDecimal totalFlow = financeManagerService.getTotalAmount(transactions);
            MonthlyTransactionTotal newMonth = new MonthlyTransactionTotal(currentMonthYear, totalFlow);
            annualMonthlyTransactions.add(newMonth);
            yearPriorToToday = yearPriorToToday.plusMonths(1);
        }
        return annualMonthlyTransactions;
    }

    public void addAccount(BankAccount account) {
        accountRepository.save(account);
    }

    public void deleteAccount(int id){
        List<Transaction> transactions = transactionRepository.findAllByAccount_Id(id);
        for (Transaction transaction: transactions) {
            transactionRepository.deleteById(transaction.getId());
        }
        accountRepository.deleteById(id);
    }

    // FM-52: startDate/endDate are optional and, when both supplied, form an inclusive range.
    // AC-2 requires the no-filter path to stay byte-for-byte unchanged, so the original
    // unfiltered/paginated query is only used when neither date is supplied; the new date-range
    // query (with an identical WHERE clause on its native countQuery, per AC-8) is used otherwise.
    public Page<Transaction> getPaginatedAccountTransactions(int id, int page, int size, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        Pageable pageable = PageRequest.of(page, size);
        if (startDate == null && endDate == null) {
            return transactionRepository.findAllByAccount_IdWithPagination(id, pageable);
        }
        return transactionRepository.findAllByAccount_IdAndDateBetweenWithPagination(id, startDate, endDate, pageable);
    }

    // FM-52: AC-3/AC-4/AC-5. Order follows the acceptance criteria as written: reject a lone
    // param first, then an inverted range, then a future date. endDate == today is valid
    // (inclusive), matching the existing future-date check in
    // TransactionService.addManualTransaction, which rejects only isAfter(LocalDate.now()).
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("Both start date and end date are required");
        }
        if (startDate == null) {
            return;
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
        if (startDate.isAfter(LocalDate.now()) || endDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date cannot be in the future");
        }
    }
}
