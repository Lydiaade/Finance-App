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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PayeeSegmentRuleRepository payeeSegmentRuleRepository;

    @Autowired
    private SegmentService segmentService;

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    // FM-23: manual "add transaction" entry point. Validation lives here (not the controller)
    // per CLAUDE.md layering - the frontend also validates, but there is no auth, so anything
    // hitting this endpoint directly must still be checked server-side.
    public TransactionResponse addManualTransaction(NewTransactionRequest request) {
        if (request.amount() == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Amount cannot be zero");
        }
        if (request.paid_to() == null || request.paid_to().isBlank()) {
            throw new IllegalArgumentException("Paid to is required");
        }
        if (request.date() == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (request.date().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date cannot be in the future");
        }

        if (request.accountId() == null) {
            throw new IllegalArgumentException("Account is required");
        }
        BankAccount account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Account does not exist"));

        // fileUpload intentionally left null - this transaction was not created via CSV import.
        // category is left null (not populated from the request) - it is a bank-provided
        // transaction-type descriptor (e.g. "Debit"/"Bill Payment") assigned only by CSVHelper on
        // CSV import, and is conceptually unrelated to the user-facing segment dropdown.
        Transaction transaction = new Transaction(
                request.date(), account, request.amount(), null, request.paid_to(), request.memo());
        if (request.segment() != null && !request.segment().isBlank()) {
            // FM-19 AC-10/AC-11/AC-12: an explicit, deliberate in-form segment choice always wins
            // over any payee rule, and going through getOrCreateSegment guarantees the same
            // case-insensitive dedup as the inline-edit path if the typed name is brand new.
            Segment segment = segmentService.getOrCreateSegment(request.segment());
            transaction.setSegment(segment.getName());
        } else {
            // FM-19 AC-10: no explicit segment supplied - fall back to an existing payee rule for
            // this exact paid_to if one exists, instead of leaving the entity's "Undefined" default.
            payeeSegmentRuleRepository.findByPaidTo(request.paid_to())
                    .ifPresent(rule -> transaction.setSegment(rule.getSegment()));
        }
        // else (no explicit segment, no matching rule): leave the entity's "Undefined" default in
        // place rather than overwriting it with null.
        Transaction saved = transactionRepository.save(transaction);
        return TransactionResponse.from(saved);
    }

    public void deleteTransaction(int id){
        transactionRepository.deleteById(id);
    }

    // FM-19 AC-4: read-only preview - counts OTHER transactions system-wide (all bank accounts)
    // sharing this transaction's exact paid_to, excluding the transaction being edited itself.
    // Deliberately has zero side effects: no rule is created/touched, no transaction is touched.
    public SegmentPreviewResponse previewSegmentChange(int transactionId) throws FileNotFoundException {
        Transaction transaction = getTransactionOrThrow(transactionId);
        long count = transactionRepository.countByPaidToExcludingId(transaction.getPaid_to(), transactionId);
        return new SegmentPreviewResponse((int) count);
    }

    // FM-19 AC-5/AC-6: commits the segment edit. Effects happen in this fixed order, and only
    // step 3 is gated by applyToExisting - steps 1 and 2 are unconditional:
    //   1. the target transaction's own segment is always updated
    //   2. the paid_to -> segment rule is always created/updated (upsert), regardless of
    //      applyToExisting - this is the ticket's actual point (ongoing classification), not an
    //      optional side-effect of the bulk-rename popup (Flag F2, flagged at PR time)
    //   3. only if applyToExisting is true, all other matching transactions are also renamed
    public UpdateTransactionSegmentResponse updateTransactionSegment(int transactionId, UpdateTransactionSegmentRequest request) throws FileNotFoundException {
        if (request.segment() == null || request.segment().isBlank()) {
            throw new IllegalArgumentException("Segment is required");
        }
        Transaction transaction = getTransactionOrThrow(transactionId);

        // AC-11/AC-12: case-insensitive dedup on segment creation, scoped to this new path only.
        Segment canonicalSegment = segmentService.getOrCreateSegment(request.segment());
        String segmentName = canonicalSegment.getName();
        String paidTo = transaction.getPaid_to();

        // 1. own transaction's segment - always updated.
        transaction.setSegment(segmentName);
        transaction = transactionRepository.save(transaction);

        // 2. paid_to -> segment rule - always created/updated (upsert), unconditional on
        // applyToExisting. This is the single most important, most easily-missed part of this
        // ticket - see class-level javadoc above and AC doc Flag F7.
        PayeeSegmentRule rule = payeeSegmentRuleRepository.findByPaidTo(paidTo)
                .orElseGet(() -> new PayeeSegmentRule(paidTo, segmentName));
        rule.setSegment(segmentName);
        payeeSegmentRuleRepository.save(rule);

        // 3. bulk-rename other matching transactions - ONLY if applyToExisting is true.
        int updatedCount = 0;
        if (request.applyToExisting()) {
            List<Transaction> others = transactionRepository.findAllByPaidToExcludingId(paidTo, transactionId);
            for (Transaction other : others) {
                other.setSegment(segmentName);
            }
            transactionRepository.saveAll(others);
            updatedCount = others.size();
        }

        return new UpdateTransactionSegmentResponse(TransactionResponse.from(transaction), updatedCount);
    }

    private Transaction getTransactionOrThrow(int transactionId) throws FileNotFoundException {
        Optional<Transaction> transaction = transactionRepository.findById(transactionId);
        if (transaction.isEmpty()) {
            throw new FileNotFoundException("This transaction does not exist");
        }
        return transaction.get();
    }
}
