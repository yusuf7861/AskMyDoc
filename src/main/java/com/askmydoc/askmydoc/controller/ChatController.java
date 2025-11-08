package com.askmydoc.askmydoc.controller;

import com.askmydoc.askmydoc.model.PageChunk;
import com.askmydoc.askmydoc.service.ai.GenerationService;
import com.askmydoc.askmydoc.service.ai.RetrievalService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    private final RetrievalService retrieval;
    private final GenerationService gen;

    @PostMapping("/ask")
    public AskResponseDto ask(@RequestBody AskRequestDto req) {
        int k = (req.top_k == null) ? 5 : req.top_k;
        List<PageChunk> chunks = retrieval.topK(req.question, req.document_ids, k);

        String context = chunks.stream().map(c -> """
      === Source: %s p.%d ===
      %s
      """.formatted(c.getDocument().getOriginalFileName(), c.getPageNumber(), c.getText())
        ).collect(Collectors.joining("\n"));

        String prompt = """
      You are a helpful assistant answering ONLY from the provided document excerpts.
      Rules:
      - Answer concisely in English.
      - If unsure, say “I couldn’t find that in the documents.”
      - Cite sources in the format: [file_name p. page_number].
      Context:
      %s

      Question:
      %s
      """.formatted(context, req.question);

        String answer = gen.generate(prompt);

        List<CitationDto> cites = chunks.stream().map(c -> {
            CitationDto cd = new CitationDto();
            cd.document_id = c.getDocument().getId();
            cd.file_name = c.getDocument().getOriginalFileName();
            cd.page_number = c.getPageNumber();
            cd.snippet = c.getText().length()>400 ? c.getText().substring(0,400)+"..." : c.getText();
            return cd;
        }).toList();

        if (Boolean.TRUE.equals(req.translate_sources)) {
            for (CitationDto c : cites) {
                c.translated_snippet = gen.generate("Translate to English concisely:\n---\n"+c.snippet);
            }
        }

        AskResponseDto resp = new AskResponseDto();
        resp.answer = answer; resp.citations = cites; return resp;
    }

    @Data
    static class AskRequestDto {
        @NotBlank public String question;
        public List<Long> document_ids;
        public Boolean translate_sources;
        public Integer top_k;
    }
    @Data
    static class AskResponseDto {
        public String answer;
        public List<CitationDto> citations;
    }
    @Data
    static class CitationDto {
        public Long document_id;
        public String file_name;
        public Integer page_number;
        public String snippet;
        public String translated_snippet;
    }
}

