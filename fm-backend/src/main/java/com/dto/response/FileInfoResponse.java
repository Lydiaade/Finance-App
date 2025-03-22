package com.dto.response;

import com.dto.BankAccount;

import java.time.LocalDateTime;
import java.util.Objects;

public class FileInfoResponse {
    private final Long id;
    private final String fileName;
    private int successfulTransactions;
    private int failedTransactions;
    private final LocalDateTime uploadedAt;
    private final BankAccount bankAccount;

    public FileInfoResponse(Long id, String fileName, int successfulTransactions, int failedTransactions, LocalDateTime uploadedAt, BankAccount bankAccount) {
        this.id = id;
        this.fileName = fileName;
        this.successfulTransactions = successfulTransactions;
        this.failedTransactions = failedTransactions;
        this.uploadedAt = uploadedAt;
        this.bankAccount = bankAccount;
    }

    public Long getId() {
        return id;
    }

    public int getSuccessfulTransactions() {
        return successfulTransactions;
    }

    public void setSuccessfulTransactions(int successfulTransactions) {
        this.successfulTransactions = successfulTransactions;
    }

    public int getFailedTransactions() {
        return failedTransactions;
    }

    public void setFailedTransactions(int failedTransactions) {
        this.failedTransactions = failedTransactions;
    }

    public String getFileName() {
        return fileName;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfoResponse response = (FileInfoResponse) o;
        return successfulTransactions == response.successfulTransactions && failedTransactions == response.failedTransactions && Objects.equals(fileName, response.fileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, successfulTransactions, failedTransactions);
    }

    @Override
    public String toString() {
        return "File Response{" +
                "fileName='" + fileName + '\'' +
                ", successfulTransactions=" + successfulTransactions +
                ", failedTransactions=" + failedTransactions +
                '}';
    }
}
