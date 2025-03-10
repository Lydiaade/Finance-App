package com.dto.response;

import java.time.LocalDateTime;
import java.util.Objects;

public class FileInfoResponse {
    private final String fileName;
    private int successfulTransactions;
    private int failedTransactions;
    private final LocalDateTime creationDate;

    public FileInfoResponse(String fileName, int successfulTransactions, int failedTransactions, LocalDateTime creationDate) {
        this.fileName = fileName;
        this.successfulTransactions = successfulTransactions;
        this.failedTransactions = failedTransactions;
        this.creationDate = creationDate;
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

    public LocalDateTime getCreationDate() {
        return creationDate;
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
