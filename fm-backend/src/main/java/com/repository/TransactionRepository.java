package com.repository;

import com.dto.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByAmountGreaterThan(Double amount);

    List<Transaction> findByAmountLessThan(Double amount);

    List<Transaction> findAllByAccount_Id(int id);

    @Query(value = "SELECT * FROM transaction WHERE account_id=?1", countQuery = "SELECT COUNT(*) FROM transaction WHERE account_id=?1", nativeQuery = true)
    Page<Transaction> findAllByAccount_IdWithPagination(int id, Pageable pageable);

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
