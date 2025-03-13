package com.dto;

import com.dto.response.FileInfoResponse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
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

    public FileUpload() {
    }

    public FileUpload(String fileName) {
        this.fileName = fileName;
        this.uploadedAt = LocalDateTime.now();
    }

    public FileUpload(Long id, String fileName, int successfulTransactions, int failedTransactions, LocalDateTime uploadedAt, List<Transaction> transactions) {
        this.id = id;
        this.fileName = fileName;
        this.successfulTransactions = successfulTransactions;
        this.failedTransactions = failedTransactions;
        this.uploadedAt = uploadedAt;
        this.transactions = transactions;
    }

    public void saveTransactionSuccess(List<Transaction> transactions) {
        this.transactions = transactions;
        this.successfulTransactions = transactions.size();
    }

    public FileInfoResponse fileInfoResponseMapper() {
        return new FileInfoResponse(id, fileName, successfulTransactions, failedTransactions, uploadedAt);
    }
}
