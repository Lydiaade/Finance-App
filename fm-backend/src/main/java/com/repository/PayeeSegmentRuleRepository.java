package com.repository;

import com.dto.PayeeSegmentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayeeSegmentRuleRepository extends JpaRepository<PayeeSegmentRule, Integer> {

    // Explicit JPQL rather than a derived findByPaidTo(...) name: the entity's actual attribute is
    // "paid_to" (underscore, matching Transaction's own field naming), and Spring Data's derived
    // query-method parser resolves method-name segments against attribute names case-sensitively
    // (e.g. "PaidTo" -> "paidTo"), which would not reliably match an attribute literally named
    // "paid_to". Using @Query sidesteps that ambiguity entirely while keeping the method name/signature
    // the ticket asks for.
    @Query("SELECT r FROM PayeeSegmentRule r WHERE r.paid_to = :paidTo")
    Optional<PayeeSegmentRule> findByPaidTo(@Param("paidTo") String paidTo);

    // FM-19 follow-up: backs the cascading rename (SegmentService.renameSegment) - every rule
    // currently pointing at this exact segment name. "segment" has no underscore, so a plain
    // derived query is unambiguous here (unlike paid_to above).
    List<PayeeSegmentRule> findAllBySegment(String segment);

    // FM-19 follow-up: backs segment delete (SegmentService.deleteSegment) - removes every rule
    // pointing at the deleted segment's name, so a stale rule can't silently reintroduce a segment
    // name that's no longer selectable on the next CSV import/manual add.
    long deleteAllBySegment(String segment);
}
