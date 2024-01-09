package com.example.UploadFilePractice.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FileUploadStatusDTO {
    String filename;
    int latestChunkIndex;
    boolean savedStatus;
}
