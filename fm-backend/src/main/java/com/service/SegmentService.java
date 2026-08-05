package com.service;

import com.dto.PayeeSegmentRule;
import com.dto.Segment;
import com.dto.Transaction;
import com.dto.response.RenameSegmentResponse;
import com.dto.response.SegmentUsageResponse;
import com.repository.PayeeSegmentRuleRepository;
import com.repository.SegmentRepository;
import com.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.util.List;

@Service
public class SegmentService {

    private static final String UNDEFINED_SEGMENT_NAME = "Undefined";

    @Autowired
    private SegmentRepository segmentRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PayeeSegmentRuleRepository payeeSegmentRuleRepository;

    public List<Segment> getAllSegments() {
        return segmentRepository.findAll();
    }

    public void addSegment(Segment segment) {
        segmentRepository.save(segment);
    }

    // FM-19: backs the two new "create inline" affordances (inline segment edit,
    // AddTransactionForm's "+ Add new segment") - deliberately NOT wired into addSegment()/the
    // existing POST /segments/segment flow, which stays a bare save with zero dedup
    // (SegmentContainer.jsx's existing add-flow is unchanged by this ticket - AC-12/Flag F4).
    // Case-insensitive: typing "groceries" when "Groceries" already exists reuses the existing
    // row (and its stored casing) rather than creating a duplicate.
    public Segment getOrCreateSegment(String name) {
        // FM-19 review follow-up: trim before the dedup lookup/save so "Groceries " (trailing
        // whitespace) matches an existing "Groceries" row instead of creating a near-duplicate.
        String trimmedName = name.trim();
        return segmentRepository.findByNameIgnoreCase(trimmedName)
                .orElseGet(() -> segmentRepository.save(new Segment(trimmedName)));
    }

    // FM-19 follow-up (project-lead feedback on PR #30): read-only usage check backing a
    // confirmation modal shown before a user renames or deletes a segment that's actually in use.
    // Counts Transaction rows whose denormalized segment string exactly equals this segment's
    // name - zero side effects.
    public SegmentUsageResponse getSegmentUsage(int segmentId) throws FileNotFoundException {
        Segment segment = getSegmentOrThrow(segmentId);
        long count = transactionRepository.countBySegment(segment.getName());
        return new SegmentUsageResponse((int) count);
    }

    // FM-19 follow-up: cascading rename. Updates the Segment's own name, every Transaction.segment
    // value currently equal to the OLD name, and every PayeeSegmentRule.segment value currently
    // equal to the OLD name (so future CSV imports/manual adds relying on that rule keep pointing
    // at a segment name that still exists) - all atomically.
    // Rejects (400 via IllegalArgumentException) a rename that collides case-insensitively with a
    // DIFFERENT existing segment - merging two segments into one is materially bigger in scope than
    // a rename and isn't what was asked for; this is the safe default, flagged for confirmation.
    // A no-op rename (same name, or only a casing change on the segment being renamed itself) is
    // allowed through and still cascades the casing update.
    @Transactional
    public RenameSegmentResponse renameSegment(int segmentId, String newName) throws FileNotFoundException {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Segment name is required");
        }
        String trimmedName = newName.trim();
        Segment segment = getSegmentOrThrow(segmentId);
        String oldName = segment.getName();

        segmentRepository.findByNameIgnoreCaseAndIdNot(trimmedName, segmentId)
                .ifPresent(other -> {
                    throw new IllegalArgumentException(
                            "A different segment named '" + other.getName() + "' already exists");
                });

        segment.setName(trimmedName);
        segment = segmentRepository.save(segment);

        List<Transaction> transactions = transactionRepository.findAllBySegment(oldName);
        for (Transaction transaction : transactions) {
            transaction.setSegment(trimmedName);
        }
        transactionRepository.saveAll(transactions);

        List<PayeeSegmentRule> rules = payeeSegmentRuleRepository.findAllBySegment(oldName);
        for (PayeeSegmentRule rule : rules) {
            rule.setSegment(trimmedName);
        }
        payeeSegmentRuleRepository.saveAll(rules);

        return new RenameSegmentResponse(segment, transactions.size(), rules.size());
    }

    // FM-19 follow-up: deleting a segment must not delete the transactions that used it - instead
    // every Transaction.segment value currently equal to this segment's name is reset back to
    // "Undefined". Also removes every PayeeSegmentRule row pointing at this segment's name,
    // unconditionally (even when the transaction usage count is zero) - otherwise a stale rule
    // could silently reintroduce a segment name that's no longer a selectable Segment on the next
    // CSV import/manual add. This rule-cleanup step is a judgment call, not explicitly requested by
    // the ticket - flagged for confirmation. Atomic: reset, rule cleanup, and the Segment row
    // deletion itself all commit or roll back together.
    @Transactional
    public void deleteSegment(int segmentId) throws FileNotFoundException {
        Segment segment = getSegmentOrThrow(segmentId);
        String name = segment.getName();

        List<Transaction> transactions = transactionRepository.findAllBySegment(name);
        for (Transaction transaction : transactions) {
            transaction.setSegment(UNDEFINED_SEGMENT_NAME);
        }
        transactionRepository.saveAll(transactions);

        payeeSegmentRuleRepository.deleteAllBySegment(name);

        segmentRepository.delete(segment);
    }

    private Segment getSegmentOrThrow(int segmentId) throws FileNotFoundException {
        return segmentRepository.findById(segmentId)
                .orElseThrow(() -> new FileNotFoundException("This segment does not exist"));
    }
}
