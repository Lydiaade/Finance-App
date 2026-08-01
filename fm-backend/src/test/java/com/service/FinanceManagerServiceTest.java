package com.service;

import com.helper.CSVHelper;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Note: this test previously called `new FinanceManagerService(repository)`, but
// FinanceManagerService only has a no-arg constructor (it uses field injection) - that call never
// compiled. Pre-existing breakage found on this branch while working FM-23, unrelated to FM-23
// itself; fixed here (switched to @InjectMocks) only so the module's test suite compiles and can
// actually be run.
public class FinanceManagerServiceTest {

    @InjectMocks
    private FinanceManagerService service;

    @Mock
    private TransactionRepository repository;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

}