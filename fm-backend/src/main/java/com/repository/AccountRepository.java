package com.repository;

import com.dto.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    List<Account> findBySortCodeAndAccountNumber(String sortCode, String accountNumber);
}
