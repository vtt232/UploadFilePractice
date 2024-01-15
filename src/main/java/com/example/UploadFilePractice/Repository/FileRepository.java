package com.example.UploadFilePractice.Repository;

import com.example.UploadFilePractice.DTO.FileDTO;
import com.example.UploadFilePractice.Entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Optional<FileEntity> findByFileName(String fileName);

    @Query("SELECT f FROM FileEntity f")
    List<FileEntity> findAllFiles();

}
