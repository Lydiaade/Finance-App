package com.repository;

import com.dto.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    List<Transaction> findByAmountGreaterThan(Double amount);

    List<Transaction> findByAmountLessThan(Double amount);

    List<Transaction> findAllByAccount_Id(Integer id);
}
