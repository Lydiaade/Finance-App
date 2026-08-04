package com.repository;

import com.dto.PayeeSegmentRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// FM-19 AC §7 backend #1 - PayeeSegmentRuleRepository: save, exact findByPaidTo, and upsert
// semantics (at most one row per paid_to).
@DataJpaTest
class PayeeSegmentRuleRepositoryTest {

    @Autowired
    private PayeeSegmentRuleRepository repository;

    @Test
    void savesAndFindsRuleByExactPaidTo() {
        repository.save(new PayeeSegmentRule("Tesco", "Groceries"));

        Optional<PayeeSegmentRule> found = repository.findByPaidTo("Tesco");

        assertTrue(found.isPresent());
        assertEquals("Groceries", found.get().getSegment());
    }

    @Test
    void findByPaidToReturnsEmptyWhenNoRuleExists() {
        Optional<PayeeSegmentRule> found = repository.findByPaidTo("Nonexistent Payee");

        assertFalse(found.isPresent());
    }

    // The crux of AC-2: editing a transaction's segment for a paid_to that already has a rule
    // must overwrite that rule's segment, never create a second row for the same paid_to.
    @Test
    void upsertOverwritesExistingRuleRatherThanCreatingADuplicateRow() {
        PayeeSegmentRule existing = repository.save(new PayeeSegmentRule("Tesco", "Groceries"));

        PayeeSegmentRule toUpdate = repository.findByPaidTo("Tesco").orElseThrow();
        toUpdate.setSegment("Household");
        repository.save(toUpdate);

        List<PayeeSegmentRule> all = repository.findAll();
        assertEquals(1, all.size());
        assertEquals(existing.getId(), all.get(0).getId());
        assertEquals("Household", all.get(0).getSegment());
        assertEquals("Household", repository.findByPaidTo("Tesco").orElseThrow().getSegment());
    }
}
