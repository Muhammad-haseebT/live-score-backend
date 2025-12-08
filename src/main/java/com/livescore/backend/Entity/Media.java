package com.livescore.backend.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Entity
@Data
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id")
    @JsonBackReference("match-media")
    private Match match;

    @Lob
    @Column(name = "file_data", columnDefinition = "bytea", nullable = false)
    private byte[] fileData;

    @Column(nullable = false)
    private String fileType;
}
