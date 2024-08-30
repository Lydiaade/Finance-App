package com.dto;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

@Entity
public class BankAccount {

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
    private BankAccountType accountType;
    private BankName bankName;
    private Currency currency;
    private BigDecimal currentBalance;
    private LocalDate currentBalanceDate;
    private boolean isMainBankAccount;

    public BankAccount(String name, String sortCode, String accountNumber, BigDecimal currentBalance, LocalDate currentBalanceDate) {
        this.name = name;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.currentBalanceDate = currentBalanceDate;
    }

    public BankAccount() {
    }

    public BankAccount(String name, String sortCode, String accountNumber, BankAccountType accountType, BankName bankName, Currency currency, BigDecimal currentBalance, LocalDate currentBalanceDate) {
        this.name = name;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.bankName = bankName;
        this.currency = currency;
        this.currentBalance = currentBalance;
        this.currentBalanceDate = currentBalanceDate;
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

    public BankAccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(BankAccountType accountType) {
        this.accountType = accountType;
    }

    public BankName getBankName() {
        return bankName;
    }

    public void setBankName(BankName bankName) {
        this.bankName = bankName;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public boolean isMainBankAccount() {
        return isMainBankAccount;
    }

    public void setMainBankAccount(boolean mainBankAccount) {
        isMainBankAccount = mainBankAccount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankAccount that = (BankAccount) o;
        return isMainBankAccount() == that.isMainBankAccount() && Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getSortCode(), that.getSortCode()) && Objects.equals(getAccountNumber(), that.getAccountNumber()) && getAccountType() == that.getAccountType() && getBankName() == that.getBankName() && Objects.equals(getCurrency(), that.getCurrency()) && Objects.equals(getCurrentBalance(), that.getCurrentBalance()) && Objects.equals(getCurrentBalanceDate(), that.getCurrentBalanceDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getSortCode(), getAccountNumber(), getAccountType(), getBankName(), getCurrency(), getCurrentBalance(), getCurrentBalanceDate(), isMainBankAccount());
    }

    @Override
    public String toString() {
        return "BankAccount{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sortCode='" + sortCode + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", accountType=" + accountType +
                ", bankName=" + bankName +
                ", currency=" + currency +
                ", currentBalance=" + currentBalance +
                ", currentBalanceDate=" + currentBalanceDate +
                ", isMainBankAccount=" + isMainBankAccount +
                '}';
    }
}