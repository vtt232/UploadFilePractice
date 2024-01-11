package com.example.UploadFilePractice.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FileDownloadChunkDTO {
    String fileName;
    int chunkIndex;

    byte[] data;
}
