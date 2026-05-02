package com.askmydoc.askmydoc.controller;

import com.askmydoc.askmydoc.model.Document;
import com.askmydoc.askmydoc.repository.DocumentRepository;
import com.askmydoc.askmydoc.repository.PageChunkRepository;
import com.askmydoc.askmydoc.service.parser.DocumentParserService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestPart("file") MultipartFile file) throws IOException, TikaException {
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds the 10 MB limit.");
        }
        Document d = parser.ingest(file);
        return Map.of("id", d.getId(), "file_name", d.getOriginalFileName(),
                "page_count", d.getPageCount(), "size_bytes", d.getSizeBytes());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> list() {
        return docRepo.findAll().stream().map(d -> Map.<String, Object>of(
                "id", d.getId(), "file_name", d.getOriginalFileName(),
                "page_count", d.getPageCount(), "size_bytes", d.getSizeBytes()
        )).toList();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) throws IOException {
        Document d = docRepo.findById(id).orElseThrow();
        Path p = Path.of(uploadDir, d.getFileName());
        Resource r = new UrlResource(p.toUri());

        // Detect actual MIME type instead of assuming PDF for all documents.
        String contentType = Files.probeContentType(p);
        MediaType mediaType = (contentType != null)
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + d.getOriginalFileName() + "\"")
                .body(r);
    }

    /** Delete a single document and all its chunks. */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteById(@PathVariable Long id) throws IOException {
        Document d = docRepo.findById(id).orElseThrow();
        long chunkCount = pageChunkRepo.countByDocumentId(id);
        pageChunkRepo.deleteByDocumentId(id);
        docRepo.delete(d);

        Path filePath = Path.of(uploadDir, d.getFileName());
        Files.deleteIfExists(filePath);

        return Map.of("status", "ok", "deleted_documents", 1, "deleted_chunks", chunkCount);
    }

    /** Delete ALL documents, their chunks, and uploaded files. */
    @DeleteMapping
    public Map<String, Object> deleteAll() throws IOException {
        long chunkCount = pageChunkRepo.count();
        long docCount = docRepo.count();

        pageChunkRepo.deleteAllInBatch();
        docRepo.deleteAllInBatch();

        Path uploadsPath = Path.of(uploadDir);
        if (Files.exists(uploadsPath) && Files.isDirectory(uploadsPath)) {
            try (var paths = Files.list(uploadsPath)) {
                paths.forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {}
                });
            }
        }

        return Map.of("status", "ok", "deleted_documents", docCount, "deleted_chunks", chunkCount);
    }
}
