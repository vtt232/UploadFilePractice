package com.example.UploadFilePractice.Controller;


import com.example.UploadFilePractice.DTO.FileDTO;
import com.example.UploadFilePractice.DTO.FileDownloadChunkDTO;
import com.example.UploadFilePractice.DTO.FileUploadStatusDTO;
import com.example.UploadFilePractice.Entity.FileEntity;
import com.example.UploadFilePractice.Service.FileService;
import jdk.jfr.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
                                                                     @RequestPart("totalChunks") String totalChunksStr,
                                                                     @RequestParam("sizeType") String sizeType) {
        log.info(chunkIndexStr);

        int chunkIndex = Integer.parseInt(chunkIndexStr);
        int totalChunks = Integer.parseInt(totalChunksStr);

        FileUploadStatusDTO fileUploadStatusDTO = new FileUploadStatusDTO();
        fileUploadStatusDTO.setFilename(fileName);
        fileUploadStatusDTO.setLatestChunkIndex(chunkIndex);

        try {
            for (MultipartFile file : files) {
                if (sizeType.equals("big")) {
                    fileService.saveFileChunkOnAzureBlob(file, fileName, chunkIndex, totalChunks);
                }
                else {
                    fileService.saveFileChunkOnDB(file, fileName, chunkIndex, totalChunks);
                }
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

    @GetMapping("/download-chunk")
    public ResponseEntity<FileDownloadChunkDTO> handleFileDownloadChunk(@RequestParam("fileName") String fileName, @RequestParam("chunkIndex") int chunkIndex) {

        try {

            FileDownloadChunkDTO fileDownloadChunkDTO = fileService.downloadFileFromAzureBlob(fileName, chunkIndex);
            log.info("Download: " + fileDownloadChunkDTO.getFileName() + " Index: " + String.valueOf(fileDownloadChunkDTO.getChunkIndex()));

            return ResponseEntity.status(200).body(fileDownloadChunkDTO);

        } catch (Exception e) {

            log.error(e.getMessage());
            return ResponseEntity.status(500).body(null);

        }

    }

    @GetMapping("/all")
    @Cacheable(value = "file-search")
    public List<FileDTO> getAllFilesFromDB() {

        try {

            return fileService.getAllFilesInDB();

        } catch (Exception e) {

            log.error(e.getMessage());
            return null;

        }

    }


}
