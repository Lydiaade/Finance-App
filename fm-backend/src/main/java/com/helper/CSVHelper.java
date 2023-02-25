package com.helper;

import com.dto.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
public class CSVHelper {
    public List<Transaction> csvToTransactions(MultipartFile file) throws IOException {
        File transformedFile = multipartFileToFile(file);
        return transformFileToTransactions(transformedFile);
    }

    public List<Transaction> transformFileToTransactions(File file) {
        List<Transaction> transactions = new ArrayList<>();
        System.out.println("About to transform file, "+ file.getName());
        try {
            Scanner scanner = new Scanner(file);
            if (scanner.hasNext()) {
                // skip header line
                scanner.nextLine();
            }
            while (scanner.hasNext()) {
                String nextLine = scanner.nextLine();
                Transaction transaction = transformSingleCSVTransaction(nextLine);
                transactions.add(transaction);
            }

        } catch (FileNotFoundException e) {
            System.out.println("Error!!!");
        } catch (Exception e) {
            System.err.print(e.getMessage());
        }
        return transactions;

    }

    private File multipartFileToFile(MultipartFile file) throws IOException {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    private Transaction transformSingleCSVTransaction(String nextLine) {
        String[] transactionValues = nextLine.split(",");
        String date = transactionValues[1];
        BigDecimal amount = new BigDecimal(transactionValues[3]);
        String category = transactionValues[4];
        String[] splitDetails = transactionValues[5].replaceAll("\"", "").split("\t");
        String paid_to = splitDetails[0].trim();
        String memo = splitDetails[1].trim();
        return new Transaction(date, amount, category, paid_to, memo);
    }
}
