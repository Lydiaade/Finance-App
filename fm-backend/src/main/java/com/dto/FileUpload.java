package com.dto;

import com.dto.response.FileInfoResponse;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "file_uploads")
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private final String fileName;
    private int successfulTransactions;
    private int failedTransactions;
    private final LocalDateTime uploadedAt;

    @OneToMany(mappedBy = "fileUpload", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    public FileUpload(String fileName) {
        this.fileName = fileName;
        this.uploadedAt = LocalDateTime.now();
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
        return new FileInfoResponse(fileName, successfulTransactions, failedTransactions, uploadedAt);
    }
}
