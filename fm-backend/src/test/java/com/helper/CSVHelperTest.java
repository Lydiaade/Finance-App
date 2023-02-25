package com.helper;

import com.dto.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CSVHelperTest {
    private final CSVHelper csvHelper;

    public CSVHelperTest() {
        csvHelper = new CSVHelper();
    }

    @Test
    public void testFileRetrieval(){
        String path = "src/test/resources/textData.csv";

        File file = new File(path);
        String absolutePath = file.getAbsolutePath();

        System.out.println(absolutePath);

        assertTrue(absolutePath.contains("src/test/resources"));
    }

    @Test
    public void fileToArray(){
        String path = "src/test/resources/textData.csv";

        File file = new File(path);
        Transaction expectedResult1 = new Transaction("31/03/2022", BigDecimal.valueOf(-9),"Debit","BAR BRUNO", "ON 29 MAR CPM");
        Transaction expectedResult2 = new Transaction("30/04/2022", BigDecimal.valueOf(-150.79),"Bill Payment","PersonA", "4929136097234001 BBP");

        List<Transaction> result = csvHelper.transformFileToTransactions(file);

        assertEquals(2, result.size());
        assertEquals(expectedResult1, result.get(0));
        assertEquals(expectedResult2, result.get(1));
    }
}
