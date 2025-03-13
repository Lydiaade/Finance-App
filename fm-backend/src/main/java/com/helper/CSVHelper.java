package com.helper;

import com.dto.BankAccount;
import com.dto.FileUpload;
import com.dto.Transaction;
import com.exceptions.UnsuccessfulTransactionRetrieval;
import com.repository.AccountRepository;
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
    private final AccountRepository accountRepository;

    public CSVHelper(AccountRepository accRepository) {
        this.accountRepository = accRepository;
    }

    public FileUpload csvToTransactions(MultipartFile file) throws IOException {
        FileUpload file_info = new FileUpload(file.getOriginalFilename());
        File transformedFile = multipartFileToFile(file);
        System.out.println("File ready to be processed");
        FileUpload upload = transformFileToTransactions(transformedFile, file_info);
        if (transformedFile.delete()) {
            System.out.println("File deleted");
        } else {
            System.err.println("File failed to delete");
        };
        return upload;
    }

    public FileUpload transformFileToTransactions(File file, FileUpload fileUpload) {
        List<Transaction> transactions = new ArrayList<>();
        System.out.println("About to transform file, " + file.getName());
        int failed_transactions = 0;
        try {
            Scanner scanner = new Scanner(file);
            if (scanner.hasNext()) {
                // skip header line
                scanner.nextLine();
            }
            while (scanner.hasNext()) {
                String nextLine = scanner.nextLine();
                try {
                    Transaction transaction = transformSingleCSVTransaction(nextLine);
                    transaction.setFileUpload(fileUpload);
                    transactions.add(transaction);
                } catch (Exception e) {
                    failed_transactions++;
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error!!!");
        }
        fileUpload.saveTransactionSuccess(transactions);
        fileUpload.setFailedTransactions(failed_transactions);
        return fileUpload;

    }

    private File multipartFileToFile(MultipartFile file) throws IOException {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    private Transaction transformSingleCSVTransaction(String nextLine) throws Exception {
        try {
            System.out.println(nextLine);
            String[] transactionValues = nextLine.split(",");
            String date = transactionValues[1];
            BankAccount account = retrieveAccountData(transactionValues[2]);
            BigDecimal amount = new BigDecimal(transactionValues[3]);
            String category = transactionValues[4];
            String[] splitDetails = transactionValues[5].replaceAll("\"", "").split("\t");
            String paid_to = splitDetails[0].trim();
            String memo = splitDetails[1].trim();
            return new Transaction(date, account, amount, category, paid_to, memo);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("A row doesn't follow the structure required");
            throw new UnsuccessfulTransactionRetrieval("Unsuccessful Transaction");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private BankAccount retrieveAccountData(String accountString) {
        System.out.println(accountString);
        String[] accountDetails = accountString.split("\s");
        System.out.println(accountDetails[0]);
        System.out.println(accountDetails[1]);
        List<BankAccount> accounts = accountRepository.findBySortCodeAndAccountNumber(accountDetails[0], accountDetails[1]);

        if (accounts.size() == 0) {
            System.out.println("Account does not exist");
            throw new IllegalArgumentException("An account needs to be created first for for the account provided.");
        }
        return accounts.get(0);
    }
}
