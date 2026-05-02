package com.askmydoc.askmydoc.service.parser;

import com.askmydoc.askmydoc.model.Document;
import com.askmydoc.askmydoc.model.PageChunk;
import com.askmydoc.askmydoc.repository.DocumentRepository;
import com.askmydoc.askmydoc.repository.PageChunkRepository;
import com.askmydoc.askmydoc.service.ai.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentParserService {
    private final TikaExtractorService tika;
    private final PdfPageExtractor pdf;
    private final Chunker chunker;
    private final EmbeddingService embeddingService;
    private final DocumentRepository docRepo;
    private final PageChunkRepository chunkRepo;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public Document ingest(org.springframework.web.multipart.MultipartFile file) throws IOException, TikaException {
        Path uploadsPath = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadsPath);

        // Use a pure UUID as the stored filename so no user-controlled data enters the path.
        // The human-readable name is preserved in the Document entity for display only.
        String stored = UUID.randomUUID().toString();
        Path path = uploadsPath.resolve(stored);

        // Verify the resolved path is strictly within the upload directory (defense-in-depth).
        if (!path.normalize().startsWith(uploadsPath)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Files.copy(file.getInputStream(), path);

        // Dedup: skip re-ingestion of identical files based on SHA-256 hash.
        String hash = sha256(path);
        Optional<Document> existing = docRepo.findByFileHash(hash);
        if (existing.isPresent()) {
            Files.deleteIfExists(path); // discard the duplicate upload
            return existing.get();
        }

        String mime = tika.detect(Files.newInputStream(path), file.getOriginalFilename());

        Document doc = new Document();
        doc.setFileName(stored);
        doc.setOriginalFileName(file.getOriginalFilename());
        doc.setSizeBytes(file.getSize());
        doc.setFileHash(hash);

        if ("application/pdf".equalsIgnoreCase(mime)) {
            int pages = pdf.getPageCount(Files.newInputStream(path));
            doc.setPageCount(pages);
            doc = docRepo.save(doc);

            List<PageChunk> chunks = new ArrayList<>();
            for (int p = 1; p <= pages; p++) {
                String pageText = pdf.extractPage(Files.newInputStream(path), p);
                for (String chunk : chunker.split(pageText, 3500, 800)) {
                    float[] emb = embeddingService.embed(chunk);
                    PageChunk pc = new PageChunk();
                    pc.setDocument(doc);
                    pc.setPageNumber(p);
                    pc.setText(chunk);
                    pc.setEmbedding(chunker.toBytes(emb));
                    pc.setTokenCount(chunk.length());
                    chunks.add(pc);
                }
            }
            chunkRepo.saveAll(chunks);
        } else {
            String text = tika.extractText(Files.newInputStream(path));
            doc.setPageCount(1);
            doc = docRepo.save(doc);

            List<PageChunk> chunks = new ArrayList<>();
            for (String chunk : chunker.split(text, 3500, 800)) {
                float[] emb = embeddingService.embed(chunk);
                PageChunk pc = new PageChunk();
                pc.setDocument(doc);
                pc.setPageNumber(1);
                pc.setText(chunk);
                pc.setEmbedding(chunker.toBytes(emb));
                pc.setTokenCount(chunk.length());
                chunks.add(pc);
            }
            chunkRepo.saveAll(chunks);
        }
        return doc;
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = new DigestInputStream(Files.newInputStream(path), md)) {
                is.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
