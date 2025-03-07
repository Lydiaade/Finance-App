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

    public Page<Transaction> getPaginatedAccountTransactions(int id, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findAllByAccount_IdWithPagination(id, pageable);
    }
}
