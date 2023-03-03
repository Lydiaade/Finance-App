package com.dto;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
public class Account {

    @Id
    @SequenceGenerator(
            name = "account_id_sequence",
            sequenceName = "account_id_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "account_id_sequence"
    )
    private Integer id;
    private String name;
    private String sortCode;
    private String accountNumber;
    private BigDecimal currentBalance;
    private LocalDate currentBalanceDate;

    public Account(String name, String sortCode, String accountNumber, BigDecimal currentBalance, LocalDate currentBalanceDate) {
        this.name = name;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.currentBalanceDate = currentBalanceDate;
    }

    public Account() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSortCode() {
        return sortCode;
    }

    public void setSortCode(String sortCode) {
        this.sortCode = sortCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public LocalDate getCurrentBalanceDate() {
        return currentBalanceDate;
    }

    public void setCurrentBalanceDate(LocalDate currentBalanceDate) {
        this.currentBalanceDate = currentBalanceDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id) && Objects.equals(name, account.name) && Objects.equals(sortCode, account.sortCode) && Objects.equals(accountNumber, account.accountNumber) && Objects.equals(currentBalance, account.currentBalance) && Objects.equals(currentBalanceDate, account.currentBalanceDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, sortCode, accountNumber, currentBalance, currentBalanceDate);
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sortCode='" + sortCode + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", currentBalance=" + currentBalance +
                ", currentBalanceDate=" + currentBalanceDate +
                '}';
    }
}