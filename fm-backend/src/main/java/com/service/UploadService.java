package com.service;

import com.dto.FileUpload;
import com.dto.response.FileInfoResponse;
import com.helper.CSVHelper;
import com.repository.FileUploadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class UploadService {
    @Autowired
    private FileUploadRepository fileUploadRepository;

    @Autowired
    private CSVHelper csvHelper;


    public FileInfoResponse saveFile(MultipartFile file) {
        try {
            System.out.println("About to save");
            FileUpload fileUpload = csvHelper.csvToTransactions(file);
            if (fileUpload.getSuccessfulTransactions() != 0) {
                fileUploadRepository.save(fileUpload);
            }

            return fileUpload.fileInfoResponseMapper();
        } catch (IOException e) {
            throw new RuntimeException("Fail to store csv data: " + e.getMessage());
        }
    }

    public List<FileUpload> getAllUploads() {
        return fileUploadRepository.findAll();
    }


    public void deleteFile(long id) {
        fileUploadRepository.deleteById(id);
    }
}
