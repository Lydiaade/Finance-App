package com.service;

import com.dto.BankAccount;
import com.dto.PayeeSegmentRule;
import com.dto.Segment;
import com.dto.Transaction;
import com.dto.request.NewTransactionRequest;
import com.dto.request.UpdateTransactionSegmentRequest;
import com.dto.response.SegmentPreviewResponse;
import com.dto.response.TransactionResponse;
import com.dto.response.UpdateTransactionSegmentResponse;
import com.repository.AccountRepository;
import com.repository.PayeeSegmentRuleRepository;
import com.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private PayeeSegmentRuleRepository payeeSegmentRuleRepository;

    @Mock
    private SegmentService segmentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // segmentService.getOrCreateSegment(...) returns a real Segment (not a smart-defaulted
    // collection type), so any test path that supplies an explicit, non-blank segment must stub
    // it - otherwise the service NPEs dereferencing a null mock return value.
    private void stubSegmentDedupToEchoInputName() {
        when(segmentService.getOrCreateSegment(anyString()))
                .thenAnswer(invocation -> new Segment(invocation.getArgument(0)));
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

    private Transaction transactionWithId(int id, BankAccount account, String paidTo, String memo, String segment) {
        return new Transaction(id, account, LocalDate.now(), BigDecimal.TEN, null, paidTo, memo, segment, null);
    }

    // AC §8 backend #3 - fully valid payload -> saved transaction returned, including generated id.
    @Test
    public void validPayloadIsSavedAndReturnedWithGeneratedId() {
        BankAccount account = accountWithId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        stubSegmentDedupToEchoInputName();

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
        stubSegmentDedupToEchoInputName();

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
        stubSegmentDedupToEchoInputName();

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
        stubSegmentDedupToEchoInputName();

        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 1, BigDecimal.valueOf(-42.10), "Groceries", "Tesco", null);
        TransactionResponse response = service.addManualTransaction(request);

        assertEquals(BigDecimal.valueOf(-42.10), response.amount());
    }

    // ---- FM-19: addManualTransaction <-> payee rule interaction (AC-10) ----

    // AC-10 - an explicit, non-blank request.segment() always wins over an existing rule for the
    // same paid_to - the rule lookup must not even be consulted.
    @Test
    public void explicitSegmentWinsOverExistingRuleOnManualAdd() {
        BankAccount account = accountWithId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubSegmentDedupToEchoInputName();

        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 1, BigDecimal.TEN, "Groceries", "Tesco", null);
        TransactionResponse response = service.addManualTransaction(request);

        assertEquals("Groceries", response.segment());
        verify(payeeSegmentRuleRepository, never()).findByPaidTo(any());
    }

    // AC-10 - blank/null request.segment() with an existing rule for that paid_to picks up the
    // rule's segment instead of leaving "Undefined".
    @Test
    public void blankSegmentWithMatchingRuleUsesRuleSegmentOnManualAdd() {
        BankAccount account = accountWithId(1);
        when(accountRepository.findById(1)).thenReturn(Optional.of(account));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payeeSegmentRuleRepository.findByPaidTo("Tesco"))
                .thenReturn(Optional.of(new PayeeSegmentRule("Tesco", "Groceries")));

        NewTransactionRequest request = new NewTransactionRequest(LocalDate.now(), 1, BigDecimal.TEN, null, "Tesco", null);
        TransactionResponse response = service.addManualTransaction(request);

        assertEquals("Groceries", response.segment());
        verify(segmentService, never()).getOrCreateSegment(any());
    }

    // ---- FM-19: GET /transactions/transaction/{id}/segment-preview ----

    // AC-4 - correct count, excluding the transaction being edited itself.
    @Test
    public void previewCountExcludesTheTransactionBeingEdited() throws FileNotFoundException {
        BankAccount account = accountWithId(1);
        Transaction target = transactionWithId(10, account, "Tesco", "Weekly shop", "Undefined");
        when(repository.findById(10)).thenReturn(Optional.of(target));
        when(repository.countByPaidToExcludingId("Tesco", 10)).thenReturn(3L);

        SegmentPreviewResponse response = service.previewSegmentChange(10);

        assertEquals(3, response.matchingTransactionCount());
    }

    // AC-4 - 0 when no other transaction shares the exact paid_to.
    @Test
    public void previewCountIsZeroWhenNoOtherTransactionSharesPaidTo() throws FileNotFoundException {
        BankAccount account = accountWithId(1);
        Transaction target = transactionWithId(10, account, "Tesco", "Weekly shop", "Undefined");
        when(repository.findById(10)).thenReturn(Optional.of(target));
        when(repository.countByPaidToExcludingId("Tesco", 10)).thenReturn(0L);

        SegmentPreviewResponse response = service.previewSegmentChange(10);

        assertEquals(0, response.matchingTransactionCount());
    }

    // AC-4 - preview must be a true read-only call: no rule created/touched, no transaction saved.
    @Test
    public void previewHasNoSideEffects() throws FileNotFoundException {
        BankAccount account = accountWithId(1);
        Transaction target = transactionWithId(10, account, "Tesco", "Weekly shop", "Undefined");
        when(repository.findById(10)).thenReturn(Optional.of(target));
        when(repository.countByPaidToExcludingId("Tesco", 10)).thenReturn(5L);

        service.previewSegmentChange(10);

        verify(repository, never()).save(any());
        verify(repository, never()).saveAll(any());
        verify(payeeSegmentRuleRepository, never()).save(any());
        verify(segmentService, never()).getOrCreateSegment(any());
    }

    // §7 backend - unknown transaction id -> a clear 404-mapped error, not a silent/incorrect result.
    @Test
    public void unknownTransactionIdOnPreviewThrowsFileNotFoundException() {
        when(repository.findById(999)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> service.previewSegmentChange(999));
    }

    // ---- FM-19: PATCH /transactions/transaction/{id}/segment ----

    // AC-6/Flag F2, applyToExisting = false case: the target transaction's own segment is still
    // updated AND the paid_to rule is still created - the rule is NOT gated by applyToExisting.
    // This is the single most easily-missed part of this ticket (AC doc Flag F7).
    @Test
    public void updateSegmentAlwaysUpdatesOwnTransactionAndCreatesRuleWhenApplyToExistingIsFalse() throws FileNotFoundException {
        BankAccount account = accountWithId(1);
        Transaction target = transactionWithId(10, account, "Tesco", "Weekly shop", "Undefined");
        when(repository.findById(10)).thenReturn(Optional.of(target));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payeeSegmentRuleRepository.findByPaidTo("Tesco")).thenReturn(Optional.empty());
        stubSegmentDedupToEchoInputName();

        UpdateTransactionSegmentResponse response = service.updateTransactionSegment(
                10, new UpdateTransactionSegmentRequest("Groceries", false));

        assertEquals("Groceries", response.transaction().segment());
        assertEquals(0, response.updatedTransactionCount());
        verify(repository, never()).findAllByPaidToExcludingId(anyString(), anyInt());

        ArgumentCaptor<PayeeSegmentRule> ruleCaptor = ArgumentCaptor.forClass(PayeeSegmentRule.class);
        verify(payeeSegmentRuleRepository).save(ruleCaptor.capture());
        assertEquals("Tesco", ruleCaptor.getValue().getPaid_to());
        assertEquals("Groceries", ruleCaptor.getValue().getSegment());
    }

    // AC-6/Flag F2, applyToExisting = true case: the rule is created regardless, tested
    // independently from the false case above per the AC doc's explicit instruction.
    @Test
    public void updateSegmentAlwaysCreatesRuleWhenApplyToExistingIsTrue() throws FileNotFoundException {
        BankAccount account = accountWithId(1);
        Transaction target = transactionWithId(10, account, "Tesco", "Weekly shop", "Undefined");
        when(repository.findById(10)).thenReturn(Optional.of(target));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payeeSegmentRuleRepository.findByPaidTo("Tesco")).thenReturn(Optional.empty());
        when(repository.findAllByPaidToExcludingId("Tesco", 10)).thenReturn(List.of());
        stubSegmentDedupToEchoInputName();

        UpdateTransactionSegmentResponse response = service.updateTransactionSegment(
                10, new UpdateTransactionSegmentRequest("Groceries", true));

        assertEquals(0, response.updatedTransactionCount());
        ArgumentCaptor<PayeeSegmentRule> ruleCaptor = ArgumentCaptor.forClass(PayeeSegmentRule.class);
        verify(payeeSegmentRuleRepository).save(ruleCaptor.capture());
        assertEquals("Groceries", ruleCaptor.getValue().getSegment());
    }

    // AC-2 - upsert: an existing rule row for this paid_to is overwritten in place, never
    // duplicated, when the segment is edited again.
    @Test
    public void updateSegmentOverwritesExistingRuleForSamePaidToRatherThanCreatingANewOne() throws FileNotFoundException {
        BankAccount account = accountWithId(1);
        Transaction target = transactionWithId(10, account, "Tesco", "Weekly shop", "Undefined");
        when(repository.findById(10)).thenReturn(Optional.of(target));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PayeeSegmentRule existingRule = new PayeeSegmentRule("Tesco", "Old Segment");
        existingRule.setId(99);
        when(payeeSegmentRuleRepository.findByPaidTo("Tesco")).thenReturn(Optional.of(existingRule));
        stubSegmentDedupToEchoInputName();

        service.updateTransactionSegment(10, new UpdateTransactionSegmentRequest("Groceries", false));

        ArgumentCaptor<PayeeSegmentRule> ruleCaptor = ArgumentCaptor.forClass(PayeeSegmentRule.class);
        verify(payeeSegmentRuleRepository).save(ruleCaptor.capture());
        assertEquals(99, ruleCaptor.getValue().getId());
        assertEquals("Groceries", ruleCaptor.getValue().getSegment());
    }

    // AC-5 step 3 - applyToExisting = true: other matching transactions are renamed and the
    // returned count reflects how many were actually renamed.
    @Test
    public void applyToExistingTrueRenamesOtherMatchingTransactions() throws FileNotFoundException {
        BankAccount account = accountWithId(1);
        Transaction target = transactionWithId(10, account, "Tesco", "Weekly shop", "Undefined");
        Transaction other1 = transactionWithId(11, account, "Tesco", "Different memo", "Undefined");
        Transaction other2 = transactionWithId(12, account, "Tesco", "Yet another", "Household");
        when(repository.findById(10)).thenReturn(Optional.of(target));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payeeSegmentRuleRepository.findByPaidTo("Tesco")).thenReturn(Optional.empty());
        when(repository.findAllByPaidToExcludingId("Tesco", 10)).thenReturn(Arrays.asList(other1, other2));
        stubSegmentDedupToEchoInputName();

        UpdateTransactionSegmentResponse response = service.updateTransactionSegment(
                10, new UpdateTransactionSegmentRequest("Groceries", true));

        assertEquals(2, response.updatedTransactionCount());
        assertEquals("Groceries", other1.getSegment());
        assertEquals("Groceries", other2.getSegment());
        verify(repository).saveAll(Arrays.asList(other1, other2));
    }

    // AC-5 step 3/AC-8 - applyToExisting = false: other matching transactions are left completely
    // unchanged, while the edited transaction's own change is still saved (AC-8's specific,
    // separately-testable "decline still saves the one edit" case).
    @Test
    public void applyToExistingFalseLeavesOtherMatchingTransactionsUnchanged() throws FileNotFoundException {
        BankAccount account = accountWithId(1);
        Transaction target = transactionWithId(10, account, "Tesco", "Weekly shop", "Undefined");
        when(repository.findById(10)).thenReturn(Optional.of(target));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payeeSegmentRuleRepository.findByPaidTo("Tesco")).thenReturn(Optional.empty());
        stubSegmentDedupToEchoInputName();

        UpdateTransactionSegmentResponse response = service.updateTransactionSegment(
                10, new UpdateTransactionSegmentRequest("Groceries", false));

        assertEquals("Groceries", response.transaction().segment());
        assertEquals(0, response.updatedTransactionCount());
        verify(repository, never()).findAllByPaidToExcludingId(anyString(), anyInt());
        verify(repository, never()).saveAll(any());
    }

    // AC-11/AC-12 - the update endpoint reuses an existing segment case-insensitively (via
    // SegmentService) rather than persisting whatever raw casing the caller sent.
    @Test
    public void updateSegmentUsesCanonicalCaseFromSegmentServiceDedup() throws FileNotFoundException {
        BankAccount account = accountWithId(1);
        Transaction target = transactionWithId(10, account, "Tesco", "Weekly shop", "Undefined");
        when(repository.findById(10)).thenReturn(Optional.of(target));
        when(repository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(payeeSegmentRuleRepository.findByPaidTo("Tesco")).thenReturn(Optional.empty());
        Segment existingSegment = new Segment("Groceries");
        existingSegment.setId(5);
        when(segmentService.getOrCreateSegment("groceries")).thenReturn(existingSegment);

        UpdateTransactionSegmentResponse response = service.updateTransactionSegment(
                10, new UpdateTransactionSegmentRequest("groceries", false));

        assertEquals("Groceries", response.transaction().segment());
    }

    // §7 backend - blank/null segment on the update endpoint is rejected before the transaction
    // is even looked up.
    @Test
    public void blankSegmentOnUpdateEndpointIsRejected() {
        UpdateTransactionSegmentRequest request = new UpdateTransactionSegmentRequest("   ", false);

        assertThrows(IllegalArgumentException.class, () -> service.updateTransactionSegment(10, request));
        verify(repository, never()).findById(any());
    }

    @Test
    public void nullSegmentOnUpdateEndpointIsRejected() {
        UpdateTransactionSegmentRequest request = new UpdateTransactionSegmentRequest(null, false);

        assertThrows(IllegalArgumentException.class, () -> service.updateTransactionSegment(10, request));
        verify(repository, never()).findById(any());
    }

    // §7 backend - unknown transaction id -> a clear 404-mapped error; no rule is created for a
    // transaction that doesn't exist.
    @Test
    public void unknownTransactionIdOnUpdateThrowsFileNotFoundException() {
        when(repository.findById(999)).thenReturn(Optional.empty());
        UpdateTransactionSegmentRequest request = new UpdateTransactionSegmentRequest("Groceries", false);

        assertThrows(FileNotFoundException.class, () -> service.updateTransactionSegment(999, request));
        verify(payeeSegmentRuleRepository, never()).save(any());
    }
}
