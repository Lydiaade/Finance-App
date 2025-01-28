package com.service;

import com.dto.Transaction;
import com.dto.FileTransferObject;
import com.dto.response.FileInfoResponse;
import com.helper.CSVHelper;
import com.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CSVHelper csvHelper;


    public FileInfoResponse saveFile(MultipartFile file) {
        try {
            System.out.println("About to save");
            FileTransferObject response = csvHelper.csvToTransactions(file);
            transactionRepository.saveAll(response.getTransactions());
            return response.getFileInfo();
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

    public void deleteTransaction(int id){
        transactionRepository.deleteById(id);
    }
}
