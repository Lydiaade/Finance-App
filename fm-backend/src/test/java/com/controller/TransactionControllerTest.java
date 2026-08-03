package com.controller;

import com.dto.request.NewTransactionRequest;
import com.dto.response.AccountSummary;
import com.dto.response.TransactionResponse;
import com.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// FM-23: the QA-identified gap - every other test for this endpoint (TransactionServiceTest)
// calls TransactionService.addManualTransaction(...) directly with hand-built Java objects,
// never exercising real Jackson (de)serialization or the actual HTTP response shape. This test
// posts real JSON through MockMvc and asserts the real 201/400 HTTP response, which is the only
// place a regression in the date-serialization fix (LocalDate + jsr310 vs the old CSV-only
// String/"d/M/yyyy" parser - the ticket's original defect, §2.3/§11 of the AC) would actually
// surface: a unit test calling the service with an already-constructed LocalDate can never catch
// a broken ISO-string-to-LocalDate deserialization path, only a real HTTP round trip can.
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    private static final String VALID_JSON = """
            {
                "date": "2024-01-15",
                "accountId": 1,
                "amount": -25.50,
                "category": "Groceries",
                "paid_to": "Tesco",
                "memo": "Weekly shop"
            }
            """;

    // AC §8 backend #3 - fully valid payload -> 201, response body contains persisted transaction
    // including generated id. Also proves the ISO date "2024-01-15" round-trips correctly end to
    // end (both into the service as a real LocalDate, and back out in the JSON body) via real
    // Jackson (de)serialization, not a hand-built Java object.
    @Test
    void validPayloadReturns201WithPersistedTransactionBody() throws Exception {
        TransactionResponse stubbedResponse = new TransactionResponse(
                42,
                LocalDate.of(2024, 1, 15),
                new AccountSummary(1, "Current Account"),
                BigDecimal.valueOf(-25.50),
                "Groceries",
                "Tesco",
                "Weekly shop"
        );
        when(transactionService.addManualTransaction(any(NewTransactionRequest.class)))
                .thenReturn(stubbedResponse);

        mockMvc.perform(post("/transactions/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.date").value("2024-01-15"))
                .andExpect(jsonPath("$.account.id").value(1))
                .andExpect(jsonPath("$.account.name").value("Current Account"))
                .andExpect(jsonPath("$.amount").value(-25.50))
                .andExpect(jsonPath("$.category").value("Groceries"))
                .andExpect(jsonPath("$.paid_to").value("Tesco"))
                .andExpect(jsonPath("$.memo").value("Weekly shop"));

        // Confirms the ISO date string in the JSON body was actually deserialized into a real
        // LocalDate (not left as a String, and not thrown away) before reaching the service -
        // this is the specific defect this test exists to guard against.
        org.mockito.ArgumentCaptor<NewTransactionRequest> captor =
                org.mockito.ArgumentCaptor.forClass(NewTransactionRequest.class);
        verify(transactionService).addManualTransaction(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2024, 1, 15), captor.getValue().date());
        org.junit.jupiter.api.Assertions.assertEquals(1, captor.getValue().accountId());
    }

    // AC §8 backend #4/#5/#6/#7 - the service rejects the request (any of the validated cases) ->
    // controller must translate IllegalArgumentException into a real 400 HTTP response with the
    // exception's message as a plain-text body, matching the existing UploadController pattern.
    @Test
    void serviceRejectionReturns400WithMessageBody() throws Exception {
        when(transactionService.addManualTransaction(any(NewTransactionRequest.class)))
                .thenThrow(new IllegalArgumentException("Amount cannot be zero"));

        mockMvc.perform(post("/transactions/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Amount cannot be zero"));
    }

    // AC §2.3/§8 backend #4 - a JSON payload that omits the "accountId" key entirely must
    // deserialize accountId as null (not a default int like 0), reach the service, and result in
    // the service's explicit "Account is required" 400 rather than an int-unboxing NPE/500 - only
    // observable via a real deserialization pass, not a hand-built NewTransactionRequest.
    @Test
    void payloadMissingAccountIdKeyDeserializesAsNullAndReturns400() throws Exception {
        String jsonMissingAccountId = """
                {
                    "date": "2024-01-15",
                    "amount": -25.50,
                    "category": "Groceries",
                    "paid_to": "Tesco",
                    "memo": "Weekly shop"
                }
                """;
        when(transactionService.addManualTransaction(any(NewTransactionRequest.class)))
                .thenThrow(new IllegalArgumentException("Account is required"));

        mockMvc.perform(post("/transactions/transaction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMissingAccountId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Account is required"));

        org.mockito.ArgumentCaptor<NewTransactionRequest> captor =
                org.mockito.ArgumentCaptor.forClass(NewTransactionRequest.class);
        verify(transactionService).addManualTransaction(captor.capture());
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().accountId());
    }
}
