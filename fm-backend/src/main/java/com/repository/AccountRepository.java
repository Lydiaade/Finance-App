package com.repository;

import com.dto.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<BankAccount, Integer> {
    List<BankAccount> findBySortCodeAndAccountNumber(String sortCode, String accountNumber);
}
