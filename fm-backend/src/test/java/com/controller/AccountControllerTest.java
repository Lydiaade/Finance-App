package com.controller;

import com.dto.Transaction;
import com.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Test
    void noFilterParamsSuppliedCallsServiceWithNullsAndReturns200() throws Exception {
        Page<Transaction> stubbedPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(accountService.getPaginatedAccountTransactions(eq(1), eq(0), eq(10), isNull(), isNull(), isNull()))
                .thenReturn(stubbedPage);

        mockMvc.perform(get("/accounts/account/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(accountService).getPaginatedAccountTransactions(1, 0, 10, null, null, null);
    }

    @Test
    void dateParamsAreParsedFromIsoStringsAndPassedToTheService() throws Exception {
        Page<Transaction> stubbedPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(accountService.getPaginatedAccountTransactions(
                eq(1), eq(0), eq(10), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), isNull()))
                .thenReturn(stubbedPage);

        mockMvc.perform(get("/accounts/account/1/transactions")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk());

        verify(accountService).getPaginatedAccountTransactions(
                1, 0, 10, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), null);
    }

    @Test
    void segmentParamAloneIsPassedThroughToTheService() throws Exception {
        Page<Transaction> stubbedPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(accountService.getPaginatedAccountTransactions(eq(1), eq(0), eq(10), isNull(), isNull(), eq("Groceries")))
                .thenReturn(stubbedPage);

        mockMvc.perform(get("/accounts/account/1/transactions")
                        .param("segment", "Groceries"))
                .andExpect(status().isOk());

        verify(accountService).getPaginatedAccountTransactions(1, 0, 10, null, null, "Groceries");
    }

    @Test
    void segmentAndDateParamsCombineOnTheSameRequest() throws Exception {
        Page<Transaction> stubbedPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(accountService.getPaginatedAccountTransactions(
                eq(1), eq(0), eq(10), eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 1, 31)), eq("Bills")))
                .thenReturn(stubbedPage);

        mockMvc.perform(get("/accounts/account/1/transactions")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("segment", "Bills"))
                .andExpect(status().isOk());

        verify(accountService).getPaginatedAccountTransactions(
                1, 0, 10, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "Bills");
    }

    // Blank/whitespace handling ("no filter" vs. literal match) is decided in the service layer,
    // not here - this only proves the controller passes the value through unmodified.
    @Test
    void blankSegmentParamIsPassedThroughUnmodified() throws Exception {
        Page<Transaction> stubbedPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(accountService.getPaginatedAccountTransactions(eq(1), eq(0), eq(10), isNull(), isNull(), eq("   ")))
                .thenReturn(stubbedPage);

        mockMvc.perform(get("/accounts/account/1/transactions")
                        .param("segment", "   "))
                .andExpect(status().isOk());

        verify(accountService).getPaginatedAccountTransactions(1, 0, 10, null, null, "   ");
    }

    @Test
    void serviceRejectionForALoneDateParamReturns400WithMessageBody() throws Exception {
        when(accountService.getPaginatedAccountTransactions(eq(1), eq(0), eq(10), eq(LocalDate.of(2024, 1, 1)), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("Both start date and end date are required"));

        mockMvc.perform(get("/accounts/account/1/transactions")
                        .param("startDate", "2024-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Both start date and end date are required"));
    }

    @Test
    void serviceRejectionForAnInvertedRangeReturns400WithMessageBody() throws Exception {
        when(accountService.getPaginatedAccountTransactions(
                eq(1), eq(0), eq(10), eq(LocalDate.of(2024, 1, 31)), eq(LocalDate.of(2024, 1, 1)), isNull()))
                .thenThrow(new IllegalArgumentException("Start date cannot be after end date"));

        mockMvc.perform(get("/accounts/account/1/transactions")
                        .param("startDate", "2024-01-31")
                        .param("endDate", "2024-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Start date cannot be after end date"));
    }

    @Test
    void serviceRejectionForAFutureDateReturns400WithMessageBody() throws Exception {
        LocalDate future = LocalDate.now().plusDays(1);
        when(accountService.getPaginatedAccountTransactions(eq(1), eq(0), eq(10), eq(future), eq(future), isNull()))
                .thenThrow(new IllegalArgumentException("Date cannot be in the future"));

        mockMvc.perform(get("/accounts/account/1/transactions")
                        .param("startDate", future.toString())
                        .param("endDate", future.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Date cannot be in the future"));
    }

    @Test
    void dateValidationStillAppliesWhenASegmentParamIsAlsoPresent() throws Exception {
        when(accountService.getPaginatedAccountTransactions(eq(1), eq(0), eq(10), eq(LocalDate.of(2024, 1, 1)), isNull(), eq("Groceries")))
                .thenThrow(new IllegalArgumentException("Both start date and end date are required"));

        mockMvc.perform(get("/accounts/account/1/transactions")
                        .param("startDate", "2024-01-01")
                        .param("segment", "Groceries"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Both start date and end date are required"));
    }
}
