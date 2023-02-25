package com.helper;

import com.dto.Account;
import com.dto.Transaction;
import com.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

@Service
public class CSVHelper {
    private final AccountRepository accountRepository;

    public CSVHelper(AccountRepository accRepository) {
        this.accountRepository = accRepository;
    }

    public List<Transaction> csvToTransactions(MultipartFile file) throws IOException {
        File transformedFile = multipartFileToFile(file);
        System.out.println("File ready to be processed");
        return transformFileToTransactions(transformedFile);
    }

    public List<Transaction> transformFileToTransactions(File file) {
        List<Transaction> transactions = new ArrayList<>();
        System.out.println("About to transform file, " + file.getName());
        try {
            Scanner scanner = new Scanner(file);
            if (scanner.hasNext()) {
                // skip header line
                scanner.nextLine();
            }
            while (scanner.hasNext()) {
                String nextLine = scanner.nextLine();
                Transaction transaction = transformSingleCSVTransaction(nextLine);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error!!!");
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
        try {
            System.out.println(nextLine);
            String[] transactionValues = nextLine.split(",");
            String date = transactionValues[1];
            Account account = retrieveAccountData(transactionValues[2]);
            BigDecimal amount = new BigDecimal(transactionValues[3]);
            String category = transactionValues[4];
            String[] splitDetails = transactionValues[5].replaceAll("\"", "").split("\t");
            String paid_to = splitDetails[0].trim();
            String memo = splitDetails[1].trim();
            return new Transaction(date, account, amount, category, paid_to, memo);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("A row doesn't follow the structure required");
            return null;
        }
    }

    private Account retrieveAccountData(String accountString) {
        System.out.println(accountString);
        String[] accountDetails = accountString.split("\s");
        System.out.println(accountDetails[0]);
        System.out.println(accountDetails[1]);
        List<Account> accounts = accountRepository.findBySortCodeAndAccountNumber(accountDetails[0], accountDetails[1]);
        System.out.println(accounts.get(0).toString());

        if (accounts.size() == 0) {
            System.out.println("Account does not exist");
            throw new IllegalArgumentException("An account needs to be created first for for the account provided.");
        }
        return accounts.get(0);
    }
}
