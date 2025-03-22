package com.repository;

import com.dto.FileUpload;
import com.dto.response.FileInfoResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
    @Query("SELECT new com.dto.response.FileInfoResponse(f.id, f.fileName, f.successfulTransactions, f.failedTransactions, f.uploadedAt, f.bankAccount) FROM FileUpload f")
    List<FileInfoResponse> getUploadCondensedData();
}
