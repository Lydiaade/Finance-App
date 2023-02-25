package com.service;

import com.dto.Transaction;
import com.helper.CSVHelper;
import com.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final CSVHelper csvHelper;

    public TransactionService(TransactionRepository transactionRepository, CSVHelper csvHelper) {
        this.transactionRepository = transactionRepository;
        this.csvHelper = csvHelper;
    }

    public void saveFile(MultipartFile file) {
        try {
            System.out.println("About to save");
            List<Transaction> csv_transactions = csvHelper.csvToTransactions(file);
            transactionRepository.saveAll(csv_transactions);
        } catch (IOException e) {
            throw new RuntimeException("Fail to store csv data: " + e.getMessage());
        }
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public void addTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
    }

    public void deleteTransaction(Integer id){
        transactionRepository.deleteById(id);
    }
}
