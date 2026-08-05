package com.service;

import com.dto.PayeeSegmentRule;
import com.dto.Segment;
import com.dto.Transaction;
import com.dto.response.RenameSegmentResponse;
import com.dto.response.SegmentUsageResponse;
import com.repository.PayeeSegmentRuleRepository;
import com.repository.SegmentRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// FM-19 AC-11/AC-12 - getOrCreateSegment backs the two new "create inline" affordances
// (inline segment edit, AddTransactionForm's "+ Add new segment"). Deliberately separate from
// addSegment()/POST /segments/segment, which stays a bare save with no dedup (Flag F4 - the
// pre-existing Segments page add-flow is unchanged by this ticket).
@ExtendWith(MockitoExtension.class)
class SegmentServiceTest {

    @InjectMocks
    private SegmentService segmentService;

    @Mock
    private SegmentRepository segmentRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PayeeSegmentRuleRepository payeeSegmentRuleRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // AC-11 - a not-yet-existing segment name is created.
    @Test
    void createsANewSegmentWhenNoCaseInsensitiveMatchExists() {
        when(segmentRepository.findByNameIgnoreCase("Groceries")).thenReturn(Optional.empty());
        when(segmentRepository.save(any(Segment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Segment result = segmentService.getOrCreateSegment("Groceries");

        assertEquals("Groceries", result.getName());
        verify(segmentRepository).save(any(Segment.class));
    }

    // AC-12 - typing a name matching an existing segment case-insensitively reuses it rather
    // than creating a duplicate, and the existing row's stored casing is preserved.
    @Test
    void reusesExistingSegmentCaseInsensitivelyWithoutCreatingADuplicate() {
        Segment existing = new Segment("Groceries");
        existing.setId(7);
        when(segmentRepository.findByNameIgnoreCase("groceries")).thenReturn(Optional.of(existing));

        Segment result = segmentService.getOrCreateSegment("groceries");

        assertEquals(7, result.getId());
        assertEquals("Groceries", result.getName());
        verify(segmentRepository, never()).save(any(Segment.class));
    }

    // Review follow-up - trailing/leading whitespace must not defeat the case-insensitive dedup:
    // "Groceries " should still match and reuse the existing "Groceries" row.
    @Test
    void trimsWhitespaceBeforeDedupLookupSoItStillMatchesAnExistingSegment() {
        Segment existing = new Segment("Groceries");
        existing.setId(7);
        when(segmentRepository.findByNameIgnoreCase("Groceries")).thenReturn(Optional.of(existing));

        Segment result = segmentService.getOrCreateSegment("Groceries ");

        assertEquals(7, result.getId());
        assertEquals("Groceries", result.getName());
        verify(segmentRepository, never()).save(any(Segment.class));
    }

    // Review follow-up - a brand-new name with surrounding whitespace is created trimmed, not
    // with the whitespace baked into the stored segment name.
    @Test
    void trimsWhitespaceBeforeCreatingANewSegment() {
        when(segmentRepository.findByNameIgnoreCase("Groceries")).thenReturn(Optional.empty());
        ArgumentCaptor<Segment> captor = ArgumentCaptor.forClass(Segment.class);
        when(segmentRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        Segment result = segmentService.getOrCreateSegment("  Groceries  ");

        assertEquals("Groceries", result.getName());
        assertEquals("Groceries", captor.getValue().getName());
    }

    // ---- FM-19 follow-up: GET /segments/segment/{id}/usage ----

    @Test
    void usageReturnsZeroWhenNoTransactionsUseTheSegment() throws FileNotFoundException {
        Segment groceries = new Segment("Groceries");
        groceries.setId(7);
        when(segmentRepository.findById(7)).thenReturn(Optional.of(groceries));
        when(transactionRepository.countBySegment("Groceries")).thenReturn(0L);

        SegmentUsageResponse response = segmentService.getSegmentUsage(7);

        assertEquals(0, response.transactionCount());
    }

    @Test
    void usageReturnsOneWhenExactlyOneTransactionUsesTheSegment() throws FileNotFoundException {
        Segment groceries = new Segment("Groceries");
        groceries.setId(7);
        when(segmentRepository.findById(7)).thenReturn(Optional.of(groceries));
        when(transactionRepository.countBySegment("Groceries")).thenReturn(1L);

        SegmentUsageResponse response = segmentService.getSegmentUsage(7);

        assertEquals(1, response.transactionCount());
    }

    @Test
    void usageReturnsCorrectCountWhenManyTransactionsUseTheSegment() throws FileNotFoundException {
        Segment groceries = new Segment("Groceries");
        groceries.setId(7);
        when(segmentRepository.findById(7)).thenReturn(Optional.of(groceries));
        when(transactionRepository.countBySegment("Groceries")).thenReturn(42L);

        SegmentUsageResponse response = segmentService.getSegmentUsage(7);

        assertEquals(42, response.transactionCount());
    }

    @Test
    void usageThrowsFileNotFoundExceptionForUnknownSegmentId() {
        when(segmentRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> segmentService.getSegmentUsage(999));
    }

    // ---- FM-19 follow-up: PATCH /segments/segment/{id} (cascading rename) ----

    @Test
    void renameCascadesToMatchingTransactionsAndPayeeSegmentRules() throws FileNotFoundException {
        Segment groceries = new Segment("Groceries");
        groceries.setId(7);
        when(segmentRepository.findById(7)).thenReturn(Optional.of(groceries));
        when(segmentRepository.findByNameIgnoreCaseAndIdNot("Food", 7)).thenReturn(Optional.empty());
        when(segmentRepository.save(any(Segment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction t1 = new Transaction();
        t1.setSegment("Groceries");
        Transaction t2 = new Transaction();
        t2.setSegment("Groceries");
        when(transactionRepository.findAllBySegment("Groceries")).thenReturn(List.of(t1, t2));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        PayeeSegmentRule rule = new PayeeSegmentRule("Tesco", "Groceries");
        when(payeeSegmentRuleRepository.findAllBySegment("Groceries")).thenReturn(List.of(rule));
        when(payeeSegmentRuleRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        RenameSegmentResponse response = segmentService.renameSegment(7, "Food");

        assertEquals("Food", response.segment().getName());
        assertEquals(2, response.updatedTransactionCount());
        assertEquals(1, response.updatedRuleCount());
        assertEquals("Food", t1.getSegment());
        assertEquals("Food", t2.getSegment());
        assertEquals("Food", rule.getSegment());
    }

    @Test
    void renameHandlesZeroLinkedTransactionsAndRulesWithoutError() throws FileNotFoundException {
        Segment groceries = new Segment("Groceries");
        groceries.setId(7);
        when(segmentRepository.findById(7)).thenReturn(Optional.of(groceries));
        when(segmentRepository.findByNameIgnoreCaseAndIdNot("Food", 7)).thenReturn(Optional.empty());
        when(segmentRepository.save(any(Segment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findAllBySegment("Groceries")).thenReturn(List.of());
        when(payeeSegmentRuleRepository.findAllBySegment("Groceries")).thenReturn(List.of());

        RenameSegmentResponse response = segmentService.renameSegment(7, "Food");

        assertEquals(0, response.updatedTransactionCount());
        assertEquals(0, response.updatedRuleCount());
        verify(transactionRepository).saveAll(List.of());
        verify(payeeSegmentRuleRepository).saveAll(List.of());
    }

    @Test
    void renameRejectsCaseInsensitiveCollisionWithADifferentExistingSegment() {
        Segment groceries = new Segment("Groceries");
        groceries.setId(7);
        Segment food = new Segment("Food");
        food.setId(9);
        when(segmentRepository.findById(7)).thenReturn(Optional.of(groceries));
        when(segmentRepository.findByNameIgnoreCaseAndIdNot("food", 7)).thenReturn(Optional.of(food));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> segmentService.renameSegment(7, "food"));

        assertTrue(ex.getMessage().contains("Food"));
        verify(segmentRepository, never()).save(any(Segment.class));
        verify(transactionRepository, never()).saveAll(anyList());
        verify(payeeSegmentRuleRepository, never()).saveAll(anyList());
    }

    @Test
    void renameAllowsRenamingASegmentToItsOwnCurrentNameWithOnlyACasingChange() throws FileNotFoundException {
        Segment groceries = new Segment("Groceries");
        groceries.setId(7);
        // the only case-insensitive match is the segment being renamed itself, excluded by id.
        when(segmentRepository.findById(7)).thenReturn(Optional.of(groceries));
        when(segmentRepository.findByNameIgnoreCaseAndIdNot("groceries", 7)).thenReturn(Optional.empty());
        when(segmentRepository.save(any(Segment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findAllBySegment("Groceries")).thenReturn(List.of());
        when(payeeSegmentRuleRepository.findAllBySegment("Groceries")).thenReturn(List.of());

        RenameSegmentResponse response = segmentService.renameSegment(7, "groceries");

        assertEquals("groceries", response.segment().getName());
    }

    @Test
    void renameThrowsIllegalArgumentExceptionForBlankName() {
        assertThrows(IllegalArgumentException.class, () -> segmentService.renameSegment(7, "  "));
        verify(segmentRepository, never()).findById(any());
    }

    @Test
    void renameThrowsFileNotFoundExceptionForUnknownSegmentId() {
        when(segmentRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> segmentService.renameSegment(999, "Food"));
    }

    // ---- FM-19 follow-up: DELETE /segments/segment/{id} ----

    @Test
    void deleteResetsLinkedTransactionsToUndefinedInsteadOfDeletingThem() throws FileNotFoundException {
        Segment groceries = new Segment("Groceries");
        groceries.setId(7);
        when(segmentRepository.findById(7)).thenReturn(Optional.of(groceries));

        Transaction t1 = new Transaction();
        t1.setSegment("Groceries");
        Transaction t2 = new Transaction();
        t2.setSegment("Groceries");
        when(transactionRepository.findAllBySegment("Groceries")).thenReturn(List.of(t1, t2));
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        segmentService.deleteSegment(7);

        assertEquals("Undefined", t1.getSegment());
        assertEquals("Undefined", t2.getSegment());
        verify(transactionRepository).saveAll(List.of(t1, t2));
        verify(segmentRepository, never()).deleteById(any());
        verify(segmentRepository).delete(groceries);
    }

    @Test
    void deleteRemovesMatchingPayeeSegmentRules() throws FileNotFoundException {
        Segment groceries = new Segment("Groceries");
        groceries.setId(7);
        when(segmentRepository.findById(7)).thenReturn(Optional.of(groceries));
        when(transactionRepository.findAllBySegment("Groceries")).thenReturn(List.of());

        segmentService.deleteSegment(7);

        verify(payeeSegmentRuleRepository).deleteAllBySegment("Groceries");
    }

    // Simple case: a segment with zero usages still works, and the only behavior change from
    // before this ticket is the now-unconditional rule-cleanup step.
    @Test
    void deleteWorksCorrectlyForASegmentWithZeroUsages() throws FileNotFoundException {
        Segment unused = new Segment("Unused");
        unused.setId(11);
        when(segmentRepository.findById(11)).thenReturn(Optional.of(unused));
        when(transactionRepository.findAllBySegment("Unused")).thenReturn(List.of());

        segmentService.deleteSegment(11);

        verify(transactionRepository).saveAll(List.of());
        verify(payeeSegmentRuleRepository).deleteAllBySegment("Unused");
        verify(segmentRepository).delete(unused);
    }

    @Test
    void deleteThrowsFileNotFoundExceptionForUnknownSegmentId() {
        when(segmentRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> segmentService.deleteSegment(999));
        verify(transactionRepository, never()).saveAll(anyList());
        verify(payeeSegmentRuleRepository, never()).deleteAllBySegment(any());
        verify(segmentRepository, never()).delete(any());
    }
}
