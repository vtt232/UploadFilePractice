package com.example.UploadFilePractice.Service;

import com.example.UploadFilePractice.Entity.FileEntity;
import com.example.UploadFilePractice.Repository.FileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
public class FileService {

    private final FileRepository fileRepository;


    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }



    private static byte[] concatenateByteArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public void saveFileChunk(MultipartFile file, String fileName, int chunkIndex, int totalChunks) throws IOException, RuntimeException {

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


}
