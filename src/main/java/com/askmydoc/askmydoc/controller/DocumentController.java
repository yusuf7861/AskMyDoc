package com.askmydoc.askmydoc.controller;

import com.askmydoc.askmydoc.model.Document;
import com.askmydoc.askmydoc.repository.DocumentRepository;
import com.askmydoc.askmydoc.service.DocumentParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentParserService parser;
    private final DocumentRepository docRepo;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> upload(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.getSize() > 10*1024*1024) throw new RuntimeException("Max 10MB");
        Document d = parser.ingest(file);
        return Map.of("id", d.getId(), "file_name", d.getOriginalFileName(),
                "page_count", d.getPageCount(), "size_bytes", d.getSizeBytes());
    }

    @GetMapping
    public List<Map<String,Object>> list() {
        return docRepo.findAll().stream().map(d -> Map.of(
                "id", d.getId(), "file_name", d.getOriginalFileName(),
                "page_count", d.getPageCount(), "size_bytes", d.getSizeBytes()
        )).toList();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) throws IOException {
        Document d = docRepo.findById(id).orElseThrow();
        Path p = Path.of("uploads", d.getFileName());
        Resource r = new UrlResource(p.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\""+d.getOriginalFileName()+"\"")
                .body(r);
    }
}
