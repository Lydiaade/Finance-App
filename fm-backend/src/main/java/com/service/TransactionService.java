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
    @Autowired
    TransactionRepository transactionRepository;

    public void saveFile(MultipartFile file) {
        try {
            List<Transaction> csv_transactions = CSVHelper.csvToTransactions(file);
            transactionRepository.saveAll(csv_transactions);
        } catch (IOException e) {
            throw new RuntimeException("fail to store csv data: " + e.getMessage());
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
