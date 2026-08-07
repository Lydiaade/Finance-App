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

// FM-19 AC-7 - matching for the preview/update-segment endpoints is exact string match on
// paid_to ONLY. memo is deliberately NOT part of the match (Amigos decision #1, overriding the
// ticket's literal wording) - this is a regression test directly proving that, since it's exactly
// where drift could creep back in. Matching is also system-wide across bank accounts, not scoped
// to one account (Amigos decision #2).
@DataJpaTest
class TransactionSegmentMatchingRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private BankAccount newAccount(String sortCode, String accountNumber) {
        return accountRepository.save(
                new BankAccount("Account " + sortCode, sortCode, accountNumber, new BigDecimal(2000), LocalDate.now()));
    }

    @Test
    void countAndListMatchOnPaidToOnlyIgnoringDifferingMemo() {
        BankAccount account = newAccount("SORTCODE1", "ACCNUMBER1");

        Transaction edited = transactionRepository.save(
                new Transaction(LocalDate.now(), account, BigDecimal.TEN, null, "Tesco", "Weekly shop"));
        Transaction samePayeeDifferentMemo = transactionRepository.save(
                new Transaction(LocalDate.now(), account, BigDecimal.TEN, null, "Tesco", "Completely different memo text"));
        Transaction differentPayee = transactionRepository.save(
                new Transaction(LocalDate.now(), account, BigDecimal.TEN, null, "Sainsburys", "Weekly shop"));

        long count = transactionRepository.countByPaidToExcludingId("Tesco", edited.getId());
        List<Transaction> matches = transactionRepository.findAllByPaidToExcludingId("Tesco", edited.getId());

        assertEquals(1, count);
        assertEquals(1, matches.size());
        assertEquals(samePayeeDifferentMemo.getId(), matches.get(0).getId());
        assertTrue(matches.stream().noneMatch(t -> t.getId() == differentPayee.getId()));
    }

    @Test
    void countIsZeroWhenNoOtherTransactionSharesExactPaidTo() {
        BankAccount account = newAccount("SORTCODE2", "ACCNUMBER2");

        Transaction edited = transactionRepository.save(
                new Transaction(LocalDate.now(), account, BigDecimal.TEN, null, "Tesco", "Weekly shop"));

        long count = transactionRepository.countByPaidToExcludingId("Tesco", edited.getId());
        List<Transaction> matches = transactionRepository.findAllByPaidToExcludingId("Tesco", edited.getId());

        assertEquals(0, count);
        assertTrue(matches.isEmpty());
    }

    @Test
    void matchingIsSystemWideAcrossBankAccountsNotScopedToOneAccount() {
        BankAccount accountOne = newAccount("SORTCODE3", "ACCNUMBER3");
        BankAccount accountTwo = newAccount("SORTCODE4", "ACCNUMBER4");

        Transaction edited = transactionRepository.save(
                new Transaction(LocalDate.now(), accountOne, BigDecimal.TEN, null, "Tesco", "Weekly shop"));
        Transaction otherAccountSamePayee = transactionRepository.save(
                new Transaction(LocalDate.now(), accountTwo, BigDecimal.TEN, null, "Tesco", "Different account, same payee"));

        long count = transactionRepository.countByPaidToExcludingId("Tesco", edited.getId());
        List<Transaction> matches = transactionRepository.findAllByPaidToExcludingId("Tesco", edited.getId());

        assertEquals(1, count);
        assertEquals(otherAccountSamePayee.getId(), matches.get(0).getId());
    }
}
