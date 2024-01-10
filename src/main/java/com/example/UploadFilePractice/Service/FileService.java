package com.example.UploadFilePractice.Service;

import com.azure.spring.cloud.core.resource.AzureStorageBlobProtocolResolver;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.specialized.AppendBlobClient;
import com.example.UploadFilePractice.Entity.FileEntity;
import com.example.UploadFilePractice.Repository.FileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

@Service
public class FileService {

    private final FileRepository fileRepository;

    private static final String diskAddress = "C:/Users/vtthinh/Desktop/file_destination/";

    private int maxRetries = 3;
    private int retryCount = 0;

    private final String containerName;
    private final ResourceLoader resourceLoader;
    private final AzureStorageBlobProtocolResolver azureStorageBlobProtocolResolver;

    private BlobServiceClient blobServiceClient;


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


}
