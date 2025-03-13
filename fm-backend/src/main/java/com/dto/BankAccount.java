package com.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Objects;

@Entity
@Setter
@Getter
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
    private int id;
    private String name;
    private String sortCode;
    private String accountNumber;
    @Enumerated(EnumType.STRING)
    private BankAccountType accountType;
    @Enumerated(EnumType.STRING)
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

    public BankAccount(String name, String sortCode, String accountNumber, String accountType, String bankName, String currency, BigDecimal currentBalance, String currentBalanceDate) {
        this.name = name;
        this.sortCode = sortCode;
        this.accountNumber = accountNumber;
        this.accountType = BankAccountType.valueOf(accountType);
        this.bankName = BankName.valueOf(bankName);
        this.currency = Currency.getInstance(currency);
        this.currentBalance = currentBalance;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        this.currentBalanceDate = LocalDate.parse(currentBalanceDate, dtf);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BankAccount that = (BankAccount) o;
        return id == that.id && isMainBankAccount == that.isMainBankAccount && Objects.equals(name, that.name) && Objects.equals(sortCode, that.sortCode) && Objects.equals(accountNumber, that.accountNumber) && accountType == that.accountType && bankName == that.bankName && Objects.equals(currency, that.currency) && Objects.equals(currentBalance, that.currentBalance) && Objects.equals(currentBalanceDate, that.currentBalanceDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, sortCode, accountNumber, accountType, bankName, currency, currentBalance, currentBalanceDate, isMainBankAccount);
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