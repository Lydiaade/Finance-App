package com.repository;

import com.dto.BankAccount;
import com.dto.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// FM-19 follow-up: real-database coverage for TransactionRepository.countBySegment and
// findAllBySegment - backing GET /segments/segment/{id}/usage, the cascading rename, and the
// reset-to-Undefined step of segment delete. Complements the Mockito-based SegmentServiceTest.
@DataJpaTest
class TransactionSegmentUsageRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private BankAccount newAccount(String sortCode, String accountNumber) {
        return accountRepository.save(
                new BankAccount("Account " + sortCode, sortCode, accountNumber, new BigDecimal(2000), LocalDate.now()));
    }

    @Test
    void countAndListAreZeroAndEmptyWhenNoTransactionUsesTheSegment() {
        BankAccount account = newAccount("SORTCODE10", "ACCNUMBER10");
        Transaction other = transactionRepository.save(
                new Transaction(LocalDate.now(), account, BigDecimal.TEN, null, "Tesco", "Weekly shop"));
        other.setSegment("Household");
        transactionRepository.save(other);

        assertEquals(0, transactionRepository.countBySegment("Groceries"));
        assertTrue(transactionRepository.findAllBySegment("Groceries").isEmpty());
    }

    @Test
    void countAndListMatchExactlyOneTransactionUsingTheSegment() {
        BankAccount account = newAccount("SORTCODE11", "ACCNUMBER11");
        Transaction transaction = transactionRepository.save(
                new Transaction(LocalDate.now(), account, BigDecimal.TEN, null, "Tesco", "Weekly shop"));
        transaction.setSegment("Groceries");
        transactionRepository.save(transaction);

        assertEquals(1, transactionRepository.countBySegment("Groceries"));
        List<Transaction> matches = transactionRepository.findAllBySegment("Groceries");
        assertEquals(1, matches.size());
        assertEquals(transaction.getId(), matches.get(0).getId());
    }

    @Test
    void countAndListMatchManyTransactionsUsingTheSameSegmentAcrossDifferentAccounts() {
        BankAccount accountOne = newAccount("SORTCODE12", "ACCNUMBER12");
        BankAccount accountTwo = newAccount("SORTCODE13", "ACCNUMBER13");

        Transaction t1 = transactionRepository.save(
                new Transaction(LocalDate.now(), accountOne, BigDecimal.TEN, null, "Tesco", "Weekly shop"));
        t1.setSegment("Groceries");
        transactionRepository.save(t1);

        Transaction t2 = transactionRepository.save(
                new Transaction(LocalDate.now(), accountTwo, BigDecimal.TEN, null, "Sainsburys", "Weekly shop"));
        t2.setSegment("Groceries");
        transactionRepository.save(t2);

        Transaction unrelated = transactionRepository.save(
                new Transaction(LocalDate.now(), accountOne, BigDecimal.TEN, null, "Netflix", "Subscription"));
        unrelated.setSegment("Entertainment");
        transactionRepository.save(unrelated);

        assertEquals(2, transactionRepository.countBySegment("Groceries"));
        assertEquals(2, transactionRepository.findAllBySegment("Groceries").size());
    }
}
