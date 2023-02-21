package com.dto;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
public class Transaction {

    @Id
    @SequenceGenerator(
            name = "transaction_id_sequence",
            sequenceName = "transaction_id_sequence"
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "transaction_id_sequence"
    )
    private Integer id;
    private String date;
    private Float amount;
    private String category;
    private String paid_to;
    private String memo;

    public Transaction(Integer id, String date, Float amount, String category, String paid_to, String memo) {
        this.id = id;
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.paid_to = paid_to;
        this.memo = memo;
    }

    public Transaction() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Float getAmount() {
        return amount;
    }

    public void setAmount(Float amount) {
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
        return Objects.equals(id, that.id) && Objects.equals(date, that.date) && Objects.equals(amount, that.amount) && Objects.equals(category, that.category) && Objects.equals(paid_to, that.paid_to) && Objects.equals(memo, that.memo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, amount, category, paid_to, memo);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", paid_to='" + paid_to + '\'' +
                ", memo='" + memo + '\'' +
                '}';
    }
}
