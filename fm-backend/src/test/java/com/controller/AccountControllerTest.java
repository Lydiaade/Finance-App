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

// FM-52: HTTP-level coverage for GET /accounts/account/{id}/transactions - complements the
// Mockito-based AccountServiceTest (business rules) by proving the controller actually parses the
// ISO "yyyy-MM-dd" query params into real LocalDates before calling the service (AC-1), leaves the
// existing page/size params and success response shape alone when no dates are supplied (AC-2),
// and translates the service's validation IllegalArgumentException into a real 400 with the
// exception's message as a plain-text body (AC-3/AC-4/AC-5), matching TransactionController's
// existing addTransaction/updateTransactionSegment pattern.
// FM-53: extended to also cover the new `segment` query param (AC-13) - absent, blank, and
// present, alone and combined with the existing date params - proving the controller passes it
// straight through to the service without altering the response shape.
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    // AC-2: no startDate/endDate/segment supplied -> service is called with null dates and null
    // segment, same page/size handling as before, 200 with the page body.
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

    // AC-1: startDate/endDate query params (ISO yyyy-MM-dd) are parsed into real LocalDates and
    // passed through to the service.
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

    // AC-5/AC-13: a segment param, on its own (no dates), is parsed and passed straight through.
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

    // AC-8: segment and date-range params combine on the same request - both are threaded through
    // to the same service call, no alternate request shape.
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

    // AC-7: a blank/whitespace-only segment param is still passed through as-is (the
    // no-filter-if-blank decision lives in the service/specification layer, not the controller) -
    // this just proves the controller doesn't reject or otherwise mangle it.
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

    // AC-3: service rejection for a lone date param must surface as a real 400 with the
    // exception's message as the body.
    @Test
    void serviceRejectionForALoneDateParamReturns400WithMessageBody() throws Exception {
        when(accountService.getPaginatedAccountTransactions(eq(1), eq(0), eq(10), eq(LocalDate.of(2024, 1, 1)), isNull(), isNull()))
                .thenThrow(new IllegalArgumentException("Both start date and end date are required"));

        mockMvc.perform(get("/accounts/account/1/transactions")
                        .param("startDate", "2024-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Both start date and end date are required"));
    }

    // AC-4: an inverted range surfaces the service's specific message via 400.
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

    // AC-5: a future date surfaces the service's specific message via 400.
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

    // AC-9: date validation still fires (and still surfaces as 400) even when a segment param is
    // also present - segment must not short-circuit the existing date validation.
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
