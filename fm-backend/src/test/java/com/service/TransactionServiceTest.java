package com.service;

import com.dto.Account;
import com.dto.Transaction;
import com.helper.CSVHelper;
import com.repository.TransactionRepository;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    private TransactionService service;

    @Mock
    private TransactionRepository repository;

    @Mock
    private CSVHelper csvHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TransactionService(repository, csvHelper);
    }

    @Test
    public void getTransactions() {
        Account account = new Account();
        // arrange
        Transaction transaction1 = new Transaction("31/03/2022", account, new BigDecimal(200), "paid out", "The other girl", "Friend Account");
        transaction1.setId(1);
        Transaction transaction2 = new Transaction("31/03/2022",account, new BigDecimal(10), "paid in", "The other boy", "Brother Account");
        transaction1.setId(2);
        List<Transaction> transactions = Arrays.asList(transaction1, transaction2);
        when(repository.findAll()).thenReturn(transactions);

        // act
        List<Transaction> actualResult = service.getAllTransactions();

        // assert
        assertEquals(2, actualResult.size());
    }

}