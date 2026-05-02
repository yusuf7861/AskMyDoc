package com.askmydoc.askmydoc.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name="documents")
@Data
public class Document {
    @Id @GeneratedValue private Long id;
    @Column(nullable=false) private String fileName;          // stored name
    @Column(nullable=false) private String originalFileName;  // display name
    @Column(nullable=false) private Integer pageCount;
    @Column(nullable=false) private Long sizeBytes;
    @Column(nullable=false) private Instant uploadedAt = Instant.now();
    /** SHA-256 hex of the file content — used to skip re-ingestion of identical files. */
    @Column(unique=true) private String fileHash;
}
