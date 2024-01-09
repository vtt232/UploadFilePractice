package com.example.UploadFilePractice.Repository;

import com.example.UploadFilePractice.Entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Optional<FileEntity> findByFileName(String fileName);

}
