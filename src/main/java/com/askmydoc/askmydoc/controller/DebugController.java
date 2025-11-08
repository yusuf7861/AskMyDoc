package com.askmydoc.askmydoc.controller;

import com.askmydoc.askmydoc.repository.DocumentRepository;
import com.askmydoc.askmydoc.repository.PageChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;

@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
public class DebugController {
    private final DocumentRepository docRepo;
    private final PageChunkRepository chunkRepo;

    @GetMapping("/stats")
    public Map<String,Object> stats() {
        return Map.of(
                "documents", docRepo.count(),
                "chunks", chunkRepo.count()
        );
    }
}

