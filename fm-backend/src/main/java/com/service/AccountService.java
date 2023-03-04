package com.service;

import com.dto.Account;
import com.dto.MonthlyTransactionTotal;
import com.dto.Transaction;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
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
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FinanceManagerService financeManagerService;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository, FinanceManagerService financeManagerService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.financeManagerService = financeManagerService;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccount(Integer id) throws FileNotFoundException {
        Optional<Account> account = accountRepository.findById(id);
        if (account.isEmpty()) {
            throw new FileNotFoundException("This account does not exist");
        }
        return account.get();
    }

    public List<Transaction> getAccountTransactions(Integer id) {
        return transactionRepository.findAllByAccount_Id(id);
    }

    public List<MonthlyTransactionTotal> getAccountAnnualMonthlyTransactions(Integer id) {
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

    public void addAccount(Account account) {
        accountRepository.save(account);
    }
}
