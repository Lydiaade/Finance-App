package com.service;

import com.dto.BankAccount;
import com.dto.Transaction;
import com.dto.request.NewTransactionRequest;
import com.dto.response.TransactionResponse;
import com.repository.AccountRepository;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @InjectMocks
    private TransactionService service;

    @Mock
    private TransactionRepository repository;

    @Mock
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getTransactions() {
        BankAccount account = new BankAccount();
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

    private BankAccount accountWithId(int id) {
        BankAccount account = new BankAccount("Current Account", "SORTCODE", "ACCNUMBER", new BigDecimal(1000), LocalDate.now());
        account.setId(id);
        return account;
    }

    private NewTransactionRequest validRequest() {
        return new NewTransactionRequest(LocalDate.now(), 1, BigDecimal.valueOf(-25.50), "Groceries", "Tesco", "Weekly shop");
    }

    // AC §8 backend #3 - fully valid payload -> saved transaction returned, including generated id.
    @Test
    public void validPayloadIsSavedAndReturnedWithGeneratedId() {
        BankAccount account = accountWithId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        when(repository.save(captor.capture())).thenAnswer(invocation -> {
            Transaction toSave = invocation.getArgument(0);
            toSave.setId(42);
            return toSave;
        });

        TransactionResponse response = service.addManualTransaction(validRequest());

        assertEquals(42, response.id());
        assertEquals(account.getId(), response.account().id());
        assertEquals(account.getName(), response.account().name());
        assertEquals(BigDecimal.valueOf(-25.50), response.amount());
        assertEquals("Groceries", response.segment());
        assertEquals("Tesco", response.paid_to());
        assertEquals("Weekly shop", response.memo());
        // category is a bank-provided descriptor populated only by CSV import - manual entries
        // must never populate it, regardless of what the segment dropdown was set to.
        assertNull(captor.getValue().getCategory());
    }

    // AC §8 backend #4 - missing/blank amount -> 400 (IllegalArgumentException from the service).
    @Test
    public void missingAmountIsRejected() {
        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 1, null, "Groceries", "Tesco", null);

        assertThrows(IllegalArgumentException.class, () -> service.addManualTransaction(request));
        verify(repository, never()).save(any());
    }

    // AC §8 backend #4 - missing/blank paid_to -> 400.
    @Test
    public void blankPaidToIsRejected() {
        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 1, BigDecimal.TEN, "Groceries", "  ", null);

        assertThrows(IllegalArgumentException.class, () -> service.addManualTransaction(request));
        verify(repository, never()).save(any());
    }

    // AC §8 backend #4 - missing/null date -> 400.
    @Test
    public void missingDateIsRejected() {
        NewTransactionRequest request = new NewTransactionRequest(null, 1, BigDecimal.TEN, "Groceries", "Tesco", null);

        assertThrows(IllegalArgumentException.class, () -> service.addManualTransaction(request));
        verify(repository, never()).save(any());
    }

    // AC §8 backend #7 - accountId not matching any existing BankAccount -> 400.
    @Test
    public void nonExistentAccountIdIsRejected() {
        when(accountRepository.findById(999)).thenReturn(Optional.empty());
        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 999, BigDecimal.TEN, "Groceries", "Tesco", null);

        assertThrows(IllegalArgumentException.class, () -> service.addManualTransaction(request));
        verify(repository, never()).save(any());
    }

    // AC §8 backend #4 - missing accountId (null, e.g. JSON payload omits the key entirely) -> 400,
    // handled by its own explicit check rather than accidentally falling through to
    // accountRepository.findById(null)/"account does not exist".
    @Test
    public void missingAccountIdIsRejected() {
        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), null, BigDecimal.TEN, "Groceries", "Tesco", null);

        assertThrows(IllegalArgumentException.class, () -> service.addManualTransaction(request));
        verify(repository, never()).save(any());
        verify(accountRepository, never()).findById(any());
    }

    // AC §8 backend #5 - amount == 0 -> 400.
    @Test
    public void zeroAmountIsRejected() {
        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 1, BigDecimal.ZERO, "Groceries", "Tesco", null);

        assertThrows(IllegalArgumentException.class, () -> service.addManualTransaction(request));
        verify(repository, never()).save(any());
    }

    // AC §8 backend #6 - date in the future -> 400.
    @Test
    public void futureDateIsRejected() {
        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now().plusDays(1), 1, BigDecimal.TEN, "Groceries", "Tesco", null);

        assertThrows(IllegalArgumentException.class, () -> service.addManualTransaction(request));
        verify(repository, never()).save(any());
    }

    // AC §8 backend #8 - segment selected -> Transaction.segment = selected value, category stays null.
    @Test
    public void selectedSegmentIsWrittenToSegmentFieldAndCategoryStaysNull() {
        BankAccount account = accountWithId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 1, BigDecimal.TEN, "Groceries", "Tesco", null);
        TransactionResponse response = service.addManualTransaction(request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(repository).save(captor.capture());
        assertEquals("Groceries", response.segment());
        assertEquals("Groceries", captor.getValue().getSegment());
        assertNull(captor.getValue().getCategory());
    }

    // AC §8 backend #9 - no segment selected -> Transaction.segment defaults to "Undefined", category stays null.
    @Test
    public void noSegmentSelectedLeavesSegmentUndefinedAndCategoryNull() {
        BankAccount account = accountWithId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 1, BigDecimal.TEN, null, "Tesco", null);
        TransactionResponse response = service.addManualTransaction(request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(repository).save(captor.capture());
        assertEquals("Undefined", response.segment());
        assertEquals("Undefined", captor.getValue().getSegment());
        assertNull(captor.getValue().getCategory());
    }

    // AC §8 backend #10 - memo omitted -> saves without error.
    @Test
    public void memoIsOptional() {
        BankAccount account = accountWithId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 1, BigDecimal.TEN, "Groceries", "Tesco", null);
        TransactionResponse response = service.addManualTransaction(request);

        assertNull(response.memo());
    }

    // AC §8 backend #11 - negative amount accepted and persisted as-is (money-out case).
    @Test
    public void negativeAmountIsAcceptedAsIs() {
        BankAccount account = accountWithId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 1, BigDecimal.valueOf(-42.10), "Groceries", "Tesco", null);
        TransactionResponse response = service.addManualTransaction(request);

        assertEquals(BigDecimal.valueOf(-42.10), response.amount());
    }
}
