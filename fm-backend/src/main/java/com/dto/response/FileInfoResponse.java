package com.dto.response;

import com.dto.Transaction;

import java.util.List;
import java.util.Objects;

public class FileInfoResponse {
    private String fileName;
    private int successfulTransactions;
    private int failedTransactions;

    public FileInfoResponse(String fileName) {
        this.fileName = fileName;
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
