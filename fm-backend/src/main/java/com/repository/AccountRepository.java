package com.repository;

import com.dto.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<BankAccount, Integer> {
    List<BankAccount> findBySortCodeAndAccountNumber(String sortCode, String accountNumber);
}
