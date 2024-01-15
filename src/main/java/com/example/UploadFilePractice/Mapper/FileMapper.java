package com.example.UploadFilePractice.Mapper;

import com.example.UploadFilePractice.DTO.FileDTO;
import com.example.UploadFilePractice.Entity.FileEntity;

public class FileMapper {

    public static FileDTO mapEntityToDTO(FileEntity fileEntity) {

        return new FileDTO(fileEntity.getFileName(), fileEntity.getData().length);

    }

}
