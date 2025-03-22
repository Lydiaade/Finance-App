package com.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CSVHelperTest {
    private CSVHelper csvHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        csvHelper = new CSVHelper();
    }

    @Test
    public void testFileRetrieval() {
        String path = "src/test/resources/textData.csv";

        File file = new File(path);
        String absolutePath = file.getAbsolutePath();

        System.out.println(absolutePath);

        assertTrue(absolutePath.contains("src/test/resources"));
    }

//    @Test
//    public void csvFileToTransactionObjects() {
//        String path = "src/test/resources/testData.csv";
//        LocalDate currentBalanceDate = LocalDate.now();
//        BankAccount account = new BankAccount("Main Account", "SORTNUMBER", "ACCNUMBER", new BigDecimal(2000), currentBalanceDate);
//        when(accRepository.findBySortCodeAndAccountNumber("SORTNUMBER", "ACCNUMBER")).thenReturn(List.of(account));
//
//        File file = new File(path);
//        FileUpload file_transfer = new FileUpload(file.getName());
//        Transaction expectedResult1 = new Transaction("31/03/2022", account, BigDecimal.valueOf(-9), "Debit", "BAR BRUNO", "ON 29 MAR CPM");
//        Transaction expectedResult2 = new Transaction("30/04/2022", account, BigDecimal.valueOf(-150.79), "Bill Payment", "PersonA", "4929136097234001 BBP");
//
//        FileUpload result = csvHelper.transformFileToTransactions(file, file_transfer);
//
//        assertEquals(2, result.getTransactions().size());
//        assertEquals(expectedResult1, result.getTransactions().get(0));
//        assertEquals(expectedResult2, result.getTransactions().get(1));
//    }

//    @Test
//    public void failsToTransformCsvFileToTransactionObjectsWhenAccountDoesNotExist() {
//        String path = "src/test/resources/testData.csv";
//        when(accRepository.findBySortCodeAndAccountNumber("SORTNUMBER", "ACCNUMBER")).thenReturn(new ArrayList<>());
//
//        File file = new File(path);
//        FileUpload file_transfer = new FileUpload(file.getName());
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> csvHelper.transformFileToTransactions(file, file_transfer));
//
//        String expectedMessage = "An account needs to be created first for for the account provided.";
//        String actualMessage = exception.getMessage();
//
//        assertTrue(actualMessage.contains(expectedMessage));
//    }
}
