package com.dto;

import com.dto.response.FileInfoResponse;

import java.util.List;
import java.util.Objects;

public class FileTransferObject {
    private FileInfoResponse fileInfo;
    private List<Transaction> transactions;

    public FileTransferObject(String fileName) {
        this.fileInfo = new FileInfoResponse(fileName);
    }

    public FileInfoResponse getFileInfo() {
        return fileInfo;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        this.fileInfo.setSuccessfulTransactions(transactions.size());
    }

    public void setFailedTransactions(int failedTransactions) {
        this.fileInfo.setFailedTransactions(failedTransactions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileTransferObject that = (FileTransferObject) o;
        return Objects.equals(fileInfo, that.fileInfo) && Objects.equals(transactions, that.transactions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileInfo, transactions);
    }

    @Override
    public String toString() {
        return "FileTransferObject{" +
                "fileInfo=" + fileInfo +
                ", transactions=" + transactions +
                '}';
    }
}
