package com.askmydoc.askmydoc.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity @Table(name="page_chunks")
@Data
public class PageChunk {
    @Id @GeneratedValue private Long id;
    @ManyToOne(optional=false) private Document document;
    @Column(nullable=false) private Integer pageNumber;      // 1-based
    @Column(nullable=false, columnDefinition = "text") private String text;
    /** Embedding stored as raw IEEE-754 float bytes (LITTLE_ENDIAN) — much more compact than CSV text. */
    @Column(columnDefinition = "bytea") private byte[] embedding;
    @Column(nullable=false) private Integer tokenCount;
}
