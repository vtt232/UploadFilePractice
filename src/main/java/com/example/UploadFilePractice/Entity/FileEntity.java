package com.example.UploadFilePractice.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String fileName;

    @Column(length = 65555)
    private byte[] data;

    @Column(columnDefinition = "boolean default false")
    private boolean completed;

}
