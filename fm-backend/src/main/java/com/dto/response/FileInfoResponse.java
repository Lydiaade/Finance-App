package com.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
public class FileInfoResponse {
    private final Long id;
    private final String fileName;
    private int successfulTransactions;
    private int failedTransactions;
    private final LocalDateTime uploadedAt;

    public FileInfoResponse(Long id, String fileName, int successfulTransactions, int failedTransactions, LocalDateTime uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.successfulTransactions = successfulTransactions;
        this.failedTransactions = failedTransactions;
        this.uploadedAt = uploadedAt;
    }

//    public Long getId() {
//        return id;
//    }
//
//    public int getSuccessfulTransactions() {
//        return successfulTransactions;
//    }
//
//    public void setSuccessfulTransactions(int successfulTransactions) {
//        this.successfulTransactions = successfulTransactions;
//    }
//
//    public int getFailedTransactions() {
//        return failedTransactions;
//    }
//
//    public void setFailedTransactions(int failedTransactions) {
//        this.failedTransactions = failedTransactions;
//    }
//
//    public String getFileName() {
//        return fileName;
//    }
//
//    public LocalDateTime getUploadedAt() {
//        return uploadedAt;
//    }

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
