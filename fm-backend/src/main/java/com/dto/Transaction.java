package com.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Getter
@Setter
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    @JsonIgnore
    private BankAccount account;

    private LocalDate date;
    private BigDecimal amount;
    private String category;
    private String paid_to;
    private String memo;
    private String segment = "Undefined";

    @ManyToOne
    @JoinColumn(name = "file_upload_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private FileUpload fileUpload;

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
        this.segment = "Undefined";
    }

    public Transaction(int id, BankAccount account, LocalDate date, BigDecimal amount, String category, String paid_to, String memo, String segment, FileUpload fileUpload) {
        this.id = id;
        this.account = account;
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.paid_to = paid_to;
        this.memo = memo;
        this.segment = segment;
        this.fileUpload = fileUpload;
    }

    public Transaction(int id, BankAccount account, LocalDate date, BigDecimal amount, String category, String paid_to, String memo, FileUpload fileUpload) {
        this.id = id;
        this.account = account;
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.paid_to = paid_to;
        this.memo = memo;
        this.fileUpload = fileUpload;
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
