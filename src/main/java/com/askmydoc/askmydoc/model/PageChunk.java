package com.askmydoc.askmydoc.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity @Table(name="page_chunks")
@Data
public class PageChunk {
    @Id @GeneratedValue private Long id;
    @ManyToOne(optional=false) private Document document;
    @Column(nullable=false) private Integer pageNumber;      // 1-based
    @Lob @Column(nullable=false) private String text;
    @Lob @Column(nullable=false) private String embeddingJson; // CSV of floats
    @Column(nullable=false) private Integer tokenCount;
}
