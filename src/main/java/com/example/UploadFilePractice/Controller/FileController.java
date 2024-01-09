package com.example.UploadFilePractice.Controller;


import com.example.UploadFilePractice.DTO.FileUploadStatusDTO;
import com.example.UploadFilePractice.Entity.FileEntity;
import com.example.UploadFilePractice.Service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@Slf4j
public class FileController {


    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }


    @PostMapping("/upload-chunk")
    public ResponseEntity<FileUploadStatusDTO> handleFileUploadChunk(@RequestPart("files") MultipartFile[] files,
                                                                     @RequestPart("fileName") String fileName,
                                                                     @RequestPart("chunkIndex") String chunkIndexStr,
                                                                     @RequestPart("totalChunks") String totalChunksStr) {
        log.info(chunkIndexStr);

        int chunkIndex = Integer.parseInt(chunkIndexStr);
        int totalChunks = Integer.parseInt(totalChunksStr);

        FileUploadStatusDTO fileUploadStatusDTO = new FileUploadStatusDTO();
        fileUploadStatusDTO.setFilename(fileName);
        fileUploadStatusDTO.setLatestChunkIndex(chunkIndex);



        try {
            for (MultipartFile file : files) {
                fileService.saveFileChunk(file, fileName, chunkIndex, totalChunks);
            }
            fileUploadStatusDTO.setSavedStatus(true);
            log.info("Save success!!! File: " + fileName + "; Chunk index: " + chunkIndex + "; Total chunks: " + totalChunks);
            return ResponseEntity.ok(fileUploadStatusDTO);
        } catch (Exception e) {
            log.error(e.getMessage());
            fileUploadStatusDTO.setSavedStatus(false);
            return ResponseEntity.status(500).body(fileUploadStatusDTO);
        }
    }

}
