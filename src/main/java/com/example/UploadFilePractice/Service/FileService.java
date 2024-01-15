package com.example.UploadFilePractice.Service;

import com.azure.spring.cloud.core.resource.AzureStorageBlobProtocolResolver;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobDownloadContentResponse;
import com.azure.storage.blob.models.BlobDownloadResponse;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.specialized.AppendBlobClient;
import com.example.UploadFilePractice.DTO.FileDTO;
import com.example.UploadFilePractice.DTO.FileDownloadChunkDTO;
import com.example.UploadFilePractice.Entity.FileEntity;
import com.example.UploadFilePractice.Mapper.FileMapper;
import com.example.UploadFilePractice.Repository.FileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.annotation.Cacheable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileService {

    private final FileRepository fileRepository;

    private static final String diskAddress = "C:/Users/vtthinh/Desktop/file_destination/";

    private int maxRetries = 3;
    private int retryCount = 0;

    private final String containerName;
    private final ResourceLoader resourceLoader;
    private final AzureStorageBlobProtocolResolver azureStorageBlobProtocolResolver;

    private BlobServiceClient blobServiceClient;


    private final static int BUFFER_SIZE = 3 * 1024 * 1024;


    public FileService(FileRepository fileRepository, @Value("${spring.cloud.azure.storage.blob.container-name}") String containerName, ResourceLoader resourceLoader, AzureStorageBlobProtocolResolver azureStorageBlobProtocolResolver, BlobServiceClient blobServiceClient) {
        this.fileRepository = fileRepository;
        this.containerName = containerName;
        this.resourceLoader = resourceLoader;
        this.azureStorageBlobProtocolResolver = azureStorageBlobProtocolResolver;
        this.blobServiceClient = blobServiceClient;
    }



    private static byte[] concatenateByteArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public void saveFileChunkOnDB(MultipartFile file, String fileName, int chunkIndex, int totalChunks) throws IOException, RuntimeException {

        if (chunkIndex == 0) {

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setData(file.getBytes());

            if (totalChunks == 1) {

                fileEntity.setCompleted(true);

            }

            try {

                fileRepository.save(fileEntity);

            } catch (Exception e)
            {

                throw new RuntimeException("Failed to save data");

            }


        }
        else if (chunkIndex > 0 && chunkIndex <= totalChunks - 1) {

            Optional<FileEntity> fileEntity = fileRepository.findByFileName(fileName);

            if (fileEntity.isPresent()){

               byte[] updatedData = concatenateByteArrays(fileEntity.get().getData(), file.getBytes());
               fileEntity.get().setData(updatedData);

                if (chunkIndex == totalChunks - 1) {

                    fileEntity.get().setCompleted(true);

                }


                try {

                    fileRepository.save(fileEntity.get());

                } catch (Exception e)
                {

                    throw new RuntimeException("Failed to save data");

                }

            }
        }
        else if (chunkIndex < 0 || chunkIndex > totalChunks - 1) {

            throw new IOException("Invalid chunk index");

        }

    }


    public void saveFileChunkOnDisk(MultipartFile file, String fileName, int chunkIndex, int totalChunks) throws IOException, RuntimeException, InterruptedException {

        if (chunkIndex == 0) {

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setDiskLink(diskAddress+fileName);


            try {

                File dest = new File(diskAddress+fileName);
                fileRepository.save(fileEntity);
                file.transferTo(dest);

            } catch (Exception e)
            {

                throw new RuntimeException("Failed to save data");

            }


        }
        else if (chunkIndex > 0 && chunkIndex <= totalChunks - 1) {

            Optional<FileEntity> fileEntity = fileRepository.findByFileName(fileName);

            if (fileEntity.isPresent()){

                while (retryCount < maxRetries) {
                    try {
                        Path path = Path.of(fileEntity.get().getDiskLink());
                        Files.write(path, file.getBytes(), StandardOpenOption.APPEND);
                        break;  // Successfully wrote to the file, exit the loop
                    } catch (IOException e) {
                        retryCount++;
                        Thread.sleep(1000);  // Introduce a short delay before retrying
                    }
                }


                if (chunkIndex == totalChunks - 1) {

                   try {

                        fileEntity.get().setCompleted(true);
                        fileRepository.save(fileEntity.get());

                    } catch (Exception e)
                    {

                        throw new RuntimeException("Failed to save data");

                    }

                }



            }
        }
        else if (chunkIndex < 0 || chunkIndex > totalChunks - 1) {

            throw new IOException("Invalid chunk index");

        }

    }

    public void saveFileChunkOnAzureBlob(MultipartFile file, String fileName, int chunkIndex, int totalChunks) throws IOException, RuntimeException, InterruptedException {

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        AppendBlobClient blobClient = containerClient.getBlobClient(fileName).getAppendBlobClient();

        if (chunkIndex == 0) {

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(fileName);
            fileEntity.setDiskLink(diskAddress+fileName);


            try {
                byte[] inputData = file.getBytes();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputData);
                blobClient.create();
                blobClient.appendBlock(byteArrayInputStream, file.getSize());
               // blobClient.(inputStream, file.getSize(), true);
                fileRepository.save(fileEntity);

            } catch (Exception e)
            {

                throw new RuntimeException("Failed to save data");

            }


        }
        else if (chunkIndex > 0 && chunkIndex <= totalChunks - 1) {

            Optional<FileEntity> fileEntity = fileRepository.findByFileName(fileName);

            if (fileEntity.isPresent()){

                while (retryCount < maxRetries) {
                    try {
                        byte[] inputData = file.getBytes();
                        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(inputData);
                        //blobClient.upload(inputStream, file.getSize(), true);
                        blobClient.appendBlock(byteArrayInputStream, file.getSize());
                        break;  // Successfully wrote to the file, exit the loop
                    } catch (IOException e) {
                        retryCount++;
                        Thread.sleep(1000);  // Introduce a short delay before retrying
                    }
                }


                if (chunkIndex == totalChunks - 1) {

                    try {

                        fileEntity.get().setCompleted(true);
                        fileRepository.save(fileEntity.get());

                    } catch (Exception e)
                    {

                        throw new RuntimeException("Failed to save data");

                    }

                }



            }
        }
        else if (chunkIndex < 0 || chunkIndex > totalChunks - 1) {

            throw new IOException("Invalid chunk index");

        }

    }

    public FileDownloadChunkDTO downloadFileFromAzureBlob(String fileName, int chunkIndex) {

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        AppendBlobClient blobClient = containerClient.getBlobClient(fileName).getAppendBlobClient();

        // Get the size of the blob
        long blobSize = blobClient.getProperties().getBlobSize();
        String blobType = blobClient.getProperties().getContentType();

        try {


            if ((long) chunkIndex * BUFFER_SIZE < blobSize) {


                BlobRange blobRange = new BlobRange((long) chunkIndex * BUFFER_SIZE, (long) BUFFER_SIZE);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                DownloadRetryOptions options = new DownloadRetryOptions().setMaxRetryRequests(maxRetries);

                blobClient.downloadStreamWithResponse(outputStream, blobRange, options, null, false, null, null);
                byte[] downloadedBytes = outputStream.toByteArray();

                FileDownloadChunkDTO fileDownloadChunkDTO = new FileDownloadChunkDTO();
                fileDownloadChunkDTO.setFileName(fileName);
                fileDownloadChunkDTO.setChunkIndex(chunkIndex);
                fileDownloadChunkDTO.setData(downloadedBytes);
                fileDownloadChunkDTO.setContentType(blobType);

                return fileDownloadChunkDTO;

            } else {

                FileDownloadChunkDTO fileDownloadChunkDTO = new FileDownloadChunkDTO();
                fileDownloadChunkDTO.setFileName(fileName);
                fileDownloadChunkDTO.setChunkIndex(-1);
                fileDownloadChunkDTO.setData(null);
                fileDownloadChunkDTO.setContentType(blobType);

                return fileDownloadChunkDTO;

            }

        } catch (Exception e) {

            throw new RuntimeException(e);

        }

    }

    public List<FileDTO> getAllFilesInDB() {

        log.info("GET ALL FILES FROM DB");

        List<FileEntity> fileEntities = fileRepository.findAllFiles();

        if (!fileEntities.isEmpty()) {

            log.info("GET ALL FILES FROM DB SUCCESSFULLY");

            List<FileDTO> fileDTOS = new ArrayList<FileDTO>();

            fileDTOS = fileEntities.stream().map(FileMapper::mapEntityToDTO).toList();

            return fileDTOS;

        } else {

            log.info("GET ALL FILES FROM DB FAILED");

            return null;

        }

    }

}
