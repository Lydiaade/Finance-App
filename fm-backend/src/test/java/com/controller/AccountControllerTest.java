package com.controller;

import com.dto.BankAccount;
import com.dto.Transaction;
import com.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    // A successful delete (including of an account that had FileUpload rows, per FM-63) must
    // return a genuine 2xx - the frontend only navigates away on a successful response.
    @Test
    void deleteAccountReturnsNoContentOnSuccess() throws Exception {
        doNothing().when(accountService).deleteAccount(1);

        mockMvc.perform(delete("/accounts/account/1"))
                .andExpect(status().isNoContent());

        verify(accountService).deleteAccount(1);
    }

    // FM-63 leaves this contract untouched: any exception raised by the service (e.g. the
    // account not existing) still surfaces as a 404, not a new error shape.
    @Test
    void deleteAccountForNonExistentIdStillReturns404() throws Exception {
        doThrow(new org.springframework.dao.EmptyResultDataAccessException(1))
                .when(accountService).deleteAccount(999);

        mockMvc.perform(delete("/accounts/account/999"))
                .andExpect(status().isNotFound());
    }

    // ---- PATCH /accounts/account/{id}/balance ----

    @Test
    void updateAccountBalanceReturns200WithUpdatedAccount() throws Exception {
        BankAccount updated = new BankAccount("Account Name", "SORT NUMBER", "ACCOUNT NUMBER",
                new BigDecimal("123.45"), LocalDate.of(2024, 6, 1));
        when(accountService.updateAccountBalance(eq(1), eq(new BigDecimal("123.45")), eq(LocalDate.of(2024, 6, 1))))
                .thenReturn(updated);

        mockMvc.perform(patch("/accounts/account/1/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentBalance": 123.45,
                                    "currentBalanceDate": "2024-06-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(123.45))
                .andExpect(jsonPath("$.currentBalanceDate").value("2024-06-01"));

        verify(accountService).updateAccountBalance(1, new BigDecimal("123.45"), LocalDate.of(2024, 6, 1));
    }

    @Test
    void updateAccountBalanceReturns404ForUnknownAccountId() throws Exception {
        when(accountService.updateAccountBalance(eq(999), any(BigDecimal.class), any(LocalDate.class)))
                .thenThrow(new FileNotFoundException("This account does not exist"));

        mockMvc.perform(patch("/accounts/account/999/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentBalance": 100.00,
                                    "currentBalanceDate": "2024-06-01"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().string("This account does not exist"));
    }

    @Test
    void updateAccountBalanceReturns400ForMissingCurrentBalance() throws Exception {
        when(accountService.updateAccountBalance(eq(1), isNull(), any(LocalDate.class)))
                .thenThrow(new IllegalArgumentException("Both currentBalance and currentBalanceDate are required"));

        mockMvc.perform(patch("/accounts/account/1/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentBalanceDate": "2024-06-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Both currentBalance and currentBalanceDate are required"));
    }

    @Test
    void updateAccountBalanceReturns400ForMissingCurrentBalanceDate() throws Exception {
        when(accountService.updateAccountBalance(eq(1), any(BigDecimal.class), isNull()))
                .thenThrow(new IllegalArgumentException("Both currentBalance and currentBalanceDate are required"));

        mockMvc.perform(patch("/accounts/account/1/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentBalance": 100.00
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Both currentBalance and currentBalanceDate are required"));
    }

    @Test
    void updateAccountBalanceAcceptsANegativeBalance() throws Exception {
        BankAccount updated = new BankAccount("Account Name", "SORT NUMBER", "ACCOUNT NUMBER",
                new BigDecimal("-50.00"), LocalDate.of(2024, 6, 1));
        when(accountService.updateAccountBalance(eq(1), eq(new BigDecimal("-50.00")), eq(LocalDate.of(2024, 6, 1))))
                .thenReturn(updated);

        mockMvc.perform(patch("/accounts/account/1/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentBalance": -50.00,
                                    "currentBalanceDate": "2024-06-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(-50.00));
    }

    @Test
    void updateAccountBalanceAcceptsAFutureBalanceDate() throws Exception {
        LocalDate future = LocalDate.now().plusYears(1);
        BankAccount updated = new BankAccount("Account Name", "SORT NUMBER", "ACCOUNT NUMBER",
                new BigDecimal("100.00"), future);
        when(accountService.updateAccountBalance(eq(1), eq(new BigDecimal("100.00")), eq(future)))
                .thenReturn(updated);

        mockMvc.perform(patch("/accounts/account/1/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentBalance": 100.00,
                                    "currentBalanceDate": "%s"
                                }
                                """.formatted(future)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalanceDate").value(future.toString()));
    }
}
