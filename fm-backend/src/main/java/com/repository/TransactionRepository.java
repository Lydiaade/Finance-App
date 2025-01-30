package com.repository;

import com.dto.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
