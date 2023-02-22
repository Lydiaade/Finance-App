package com.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FinanceManagerServiceTest {

    private final FinanceManagerService service;

    public FinanceManagerServiceTest() {
        service = new FinanceManagerService();
    }


    @Test
    public void getCurrentSalary() {
        // arrange
        int expectedResult = 3000;

        // act
        int actualResult = service.getCurrentSalary();

        // assert
        assertEquals(expectedResult, actualResult);
    }

}