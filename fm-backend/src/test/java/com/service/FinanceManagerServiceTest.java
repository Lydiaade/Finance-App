package com.service;

import com.helper.CSVHelper;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FinanceManagerServiceTest {

    private FinanceManagerService service;

    @Mock
    private TransactionRepository repository;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new FinanceManagerService(repository);
    }

}