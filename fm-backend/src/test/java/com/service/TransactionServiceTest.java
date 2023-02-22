package com.service;

import com.dto.Transaction;
import com.helper.CSVHelper;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

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
        // arrange
        Transaction transaction1 = new Transaction("2022-01-01", 200.0, "paid out", "The other girl", "Friend Account");
        transaction1.setId(1);
        Transaction transaction2 = new Transaction("2022-01-01", 10.0, "paid in", "The other boy", "Brother Account");
        transaction1.setId(2);
        List<Transaction> transactions = Arrays.asList(transaction1, transaction2);
        when(repository.findAll()).thenReturn(transactions);

        // act
        List<Transaction> actualResult = service.getAllTransactions();

        // assert
        assertEquals(2, actualResult.size());
    }

}