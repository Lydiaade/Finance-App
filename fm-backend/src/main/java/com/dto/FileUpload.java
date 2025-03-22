package com.dto;

import com.dto.response.FileInfoResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "file_uploads")
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private int successfulTransactions;
    private int failedTransactions;
    private LocalDateTime uploadedAt;

    @OneToMany(mappedBy = "fileUpload", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id")
    @JsonIgnore
    private BankAccount bankAccount;

    public FileUpload() {
    }

    public FileUpload(String fileName, BankAccount bankAccount) {
        this.fileName = fileName;
        this.uploadedAt = LocalDateTime.now();
        this.bankAccount = bankAccount;
    }

    public FileUpload(Long id, String fileName, int successfulTransactions, int failedTransactions, LocalDateTime uploadedAt, List<Transaction> transactions, BankAccount bankAccount) {
        this.id = id;
        this.fileName = fileName;
        this.successfulTransactions = successfulTransactions;
        this.failedTransactions = failedTransactions;
        this.uploadedAt = uploadedAt;
        this.transactions = transactions;
        this.bankAccount = bankAccount;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        this.successfulTransactions = transactions.size();
    }

    public void setFailedTransactions(int failedTransactions) {
        this.failedTransactions = failedTransactions;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public int getSuccessfulTransactions() {
        return successfulTransactions;
    }

    public int getFailedTransactions() {
        return failedTransactions;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public FileInfoResponse fileInfoResponseMapper() {
        return new FileInfoResponse(id, fileName, successfulTransactions, failedTransactions, uploadedAt);
    }
}
