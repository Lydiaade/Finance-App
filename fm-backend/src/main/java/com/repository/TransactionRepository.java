package com.repository;

import com.dto.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// FM-53: extends JpaSpecificationExecutor so the account-scoped, paginated transactions query
// (previously two separate hand-written native queries from FM-52, each with its own
// separately-maintained native countQuery string) can be expressed as a single
// Specification<Transaction> - see TransactionSpecifications. Spring Data derives the count query
// automatically from the Specification, so there is no longer a hand-written countQuery to drift
// out of sync with the row-fetching query.
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByAmountGreaterThan(Double amount);

    List<Transaction> findByAmountLessThan(Double amount);

    List<Transaction> findAllByAccount_Id(int id);

    @Query(value = "SELECT * FROM transaction WHERE account_id=?1 AND EXTRACT('month' from date) = ?2 AND EXTRACT('year' from date) = ?3", nativeQuery = true)
    List<Transaction> findAllByAccount_IdAndDateInMonthYear(int id, int month, int year);

    // FM-19: system-wide (not scoped to one bank account) - exact match on paid_to only, memo is
    // deliberately not part of the match (Amigos decision #1). Excludes the transaction being
    // edited itself via the id-not-equal clause. JPQL (not a derived findBy... name) is used here
    // for the same reason as PayeeSegmentRuleRepository.findByPaidTo - "paid_to" is the entity's
    // actual attribute name (underscore included), which derived-name parsing doesn't reliably match.
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.paid_to = :paidTo AND t.id <> :excludeId")
    long countByPaidToExcludingId(@Param("paidTo") String paidTo, @Param("excludeId") int excludeId);

    @Query("SELECT t FROM Transaction t WHERE t.paid_to = :paidTo AND t.id <> :excludeId")
    List<Transaction> findAllByPaidToExcludingId(@Param("paidTo") String paidTo, @Param("excludeId") int excludeId);

    // FM-19 follow-up: backs GET /segments/segment/{id}/usage - "segment" has no underscore, so a
    // plain derived query is unambiguous here (same reasoning as SegmentRepository.findByNameIgnoreCase).
    long countBySegment(String segment);

    // FM-19 follow-up: backs both the cascading rename (SegmentService.renameSegment) and the
    // reset-to-Undefined step of segment delete (SegmentService.deleteSegment) - every transaction
    // currently carrying this exact segment name.
    List<Transaction> findAllBySegment(String segment);
}
