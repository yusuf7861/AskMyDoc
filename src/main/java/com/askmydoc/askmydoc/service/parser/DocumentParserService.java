package com.askmydoc.askmydoc.service.parser;

import com.askmydoc.askmydoc.model.Document;
import com.askmydoc.askmydoc.model.PageChunk;
import com.askmydoc.askmydoc.repository.DocumentRepository;
import com.askmydoc.askmydoc.repository.PageChunkRepository;
import com.askmydoc.askmydoc.service.ai.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
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

    public Document ingest(org.springframework.web.multipart.MultipartFile file) throws IOException, TikaException {
        Files.createDirectories(Path.of("uploads"));
        String stored = UUID.randomUUID()+"-"+file.getOriginalFilename();
        Path path = Path.of("uploads", stored);
        Files.copy(file.getInputStream(), path);

        String mime = tika.detect(Files.newInputStream(path), file.getOriginalFilename());

        Document doc = new Document();
        doc.setFileName(stored);
        doc.setOriginalFileName(file.getOriginalFilename());
        doc.setSizeBytes(file.getSize());

        if ("application/pdf".equalsIgnoreCase(mime)) {
            int pages = pdf.getPageCount(Files.newInputStream(path));
            doc.setPageCount(pages);
            doc = docRepo.save(doc);

            for (int p=1; p<=pages; p++) {
                String pageText = pdf.extractPage(Files.newInputStream(path), p);
                for (String chunk : chunker.split(pageText, 3500, 800)) {
                    float[] emb = embeddingService.embed(chunk);
                    PageChunk pc = new PageChunk();
                    pc.setDocument(doc); pc.setPageNumber(p);
                    pc.setText(chunk); pc.setEmbeddingJson(chunker.toCsv(emb));
                    pc.setTokenCount(chunk.length());
                    chunkRepo.save(pc);
                }
            }
        } else {
            String text = tika.extractText(Files.newInputStream(path));
            doc.setPageCount(1);
            doc = docRepo.save(doc);
            for (String chunk : chunker.split(text, 3500, 800)) {
                float[] emb = embeddingService.embed(chunk);
                PageChunk pc = new PageChunk();
                pc.setDocument(doc); pc.setPageNumber(1);
                pc.setText(chunk); pc.setEmbeddingJson(chunker.toCsv(emb));
                pc.setTokenCount(chunk.length());
                chunkRepo.save(pc);
            }
        }
        return doc;
    }
}

