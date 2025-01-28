package com.dto;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
public class Transaction {

    @Id
    @SequenceGenerator(
            name = "transaction_id_sequence",
            sequenceName = "transaction_id_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "transaction_id_sequence"
    )
    private int id;

    @ManyToOne(fetch = FetchType.EAGER)
    private BankAccount account;

    private LocalDate date;
    private BigDecimal amount;
    private String category;
    private String paid_to;
    private String memo;
    private String segment = "Undefined";

    public Transaction(String date, BankAccount account, BigDecimal amount, String category, String paid_to, String memo) {
        this.date = transformStringToDate(date);
        this.account = account;
        this.amount = amount;
        this.category = category;
        this.paid_to = paid_to;
        this.memo = memo;
    }

    public Transaction(String date, BankAccount account, BigDecimal amount, String category, String paid_to, String memo, Segment segment) {
        this.date = transformStringToDate(date);
        this.account = account;
        this.amount = amount;
        this.category = category;
        this.paid_to = paid_to;
        this.memo = memo;
        this.segment = segment.getName();
    }

    public Transaction() {
    }

    private LocalDate transformStringToDate(String date) {
        String[] dateList = date.split("/");
        int day = Integer.parseInt(dateList[0]);
        int month = Integer.parseInt(dateList[1]);
        int year = Integer.parseInt(dateList[2]);
        return LocalDate.of(year, month, day);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BankAccount getAccount() {
        return account;
    }

    public void setAccount(BankAccount account) {
        this.account = account;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment.getName();
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPaid_to() {
        return paid_to;
    }

    public void setPaid_to(String paid_to) {
        this.paid_to = paid_to;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id) && Objects.equals(account, that.account) && Objects.equals(segment, that.segment) && Objects.equals(date, that.date) && Objects.equals(amount, that.amount) && Objects.equals(category, that.category) && Objects.equals(paid_to, that.paid_to) && Objects.equals(memo, that.memo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, account, segment, date, amount, category, paid_to, memo);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", account=" + account +
                ", segment=" + segment +
                ", date=" + date +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", paid_to='" + paid_to + '\'' +
                ", memo='" + memo + '\'' +
                '}';
    }
}
