package com.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class MonthlyTransactionTotal {

    private String name;
    private BigDecimal total;

    public MonthlyTransactionTotal(String name, BigDecimal total) {
        this.name = name;
        this.total = total;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonthlyTransactionTotal that = (MonthlyTransactionTotal) o;
        return Objects.equals(name, that.name) && Objects.equals(total, that.total);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, total);
    }

    @Override
    public String toString() {
        return "MonthlyTransactionTotal{" +
                "name='" + name + '\'' +
                ", total=" + total +
                '}';
    }
}