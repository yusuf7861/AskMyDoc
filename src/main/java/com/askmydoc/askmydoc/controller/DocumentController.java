package com.askmydoc.askmydoc.controller;

import com.askmydoc.askmydoc.model.Document;
import com.askmydoc.askmydoc.repository.DocumentRepository;
import com.askmydoc.askmydoc.repository.PageChunkRepository;
import com.askmydoc.askmydoc.service.parser.DocumentParserService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
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
    private final PageChunkRepository pageChunkRepo;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,Object> upload(@RequestPart("file") MultipartFile file) throws IOException, TikaException {
        if (file.getSize() > 10*1024*1024) throw new RuntimeException("Max 10MB");
        Document d = parser.ingest(file);
        return Map.of("id", d.getId(), "file_name", d.getOriginalFileName(),
                "page_count", d.getPageCount(), "size_bytes", d.getSizeBytes());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String,Object>> list() {
        return docRepo.findAll().stream().map(d -> Map.<String,Object>of(
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

    // Deletes all documents, chunks, and uploaded files.
    @DeleteMapping
    private Map<String,Object> doClearAll() throws IOException {
        long chunkCount = pageChunkRepo.count();
        long docCount = docRepo.count();

        pageChunkRepo.deleteAllInBatch();
        docRepo.deleteAllInBatch();

        Path uploadsDir = Path.of("uploads");
        if (Files.exists(uploadsDir) && Files.isDirectory(uploadsDir)) {
            try (var paths = Files.list(uploadsDir)) {
                paths.forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {}
                });
            }
        }

        return Map.of(
                "status", "ok",
                "deleted_documents", docCount,
                "deleted_chunks", chunkCount
        );
    }
}
