package com.repository;

import com.dto.BankAccount;
import com.dto.FileUpload;
import com.dto.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// FM-23 §1/§8 (backend items 1-2): confirms the DDL change (Transaction.fileUpload's
// @JoinColumn now nullable = true) actually behaves as intended at the DB level -
// manual transactions can persist with a null FK, and a transaction created the CSV-import
// way still correctly links a non-null FileUpload.
@DataJpaTest
class TransactionPersistenceTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private FileUploadRepository fileUploadRepository;

    @Test
    void manuallyAddedTransactionPersistsWithNullFileUpload() {
        BankAccount account = accountRepository.save(
                new BankAccount("Current Account", "SORTCODE", "ACCNUMBER", new BigDecimal(2000), LocalDate.now()));

        Transaction transaction = new Transaction(
                LocalDate.now(), account, BigDecimal.valueOf(-25.50), "Groceries", "Tesco", "Weekly shop");
        // fileUpload deliberately left unset (null) - this is the manual-entry path.

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        Optional<Transaction> retrieved = transactionRepository.findById(saved.getId());
        assertNotNull(retrieved.orElse(null));
        assertNull(retrieved.get().getFileUpload());
    }

    @Test
    void csvImportedTransactionStillRequiresAndLinksAFileUpload() {
        BankAccount account = accountRepository.save(
                new BankAccount("Current Account", "SORTCODE", "ACCNUMBER", new BigDecimal(2000), LocalDate.now()));

        FileUpload fileUpload = new FileUpload("statement.csv", account);
        fileUploadRepository.save(fileUpload);

        Transaction transaction = new Transaction(
                "31/03/2022", account, BigDecimal.valueOf(-9), "Debit", "BAR BRUNO", "ON 29 MAR CPM");
        transaction.setFileUpload(fileUpload);

        Transaction saved = transactionRepository.saveAndFlush(transaction);

        Optional<Transaction> retrieved = transactionRepository.findById(saved.getId());
        assertNotNull(retrieved.orElse(null));
        assertNotNull(retrieved.get().getFileUpload());
        assertEquals(fileUpload.getId(), retrieved.get().getFileUpload().getId());
    }
}
