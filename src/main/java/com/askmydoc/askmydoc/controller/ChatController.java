package com.askmydoc.askmydoc.controller;

import com.askmydoc.askmydoc.model.PageChunk;
import com.askmydoc.askmydoc.service.ai.GenerationService;
import com.askmydoc.askmydoc.service.ai.RetrievalService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // Construct numbered context
        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            PageChunk c = chunks.get(i);
            contextBuilder.append(String.format("""
=== Source ID: %d (File: %s, Page: %d) ===
%s

""", i + 1, c.getDocument().getOriginalFileName(), c.getPageNumber(), c.getText()));
                    }

        // STRICT RAG prompt
        String prompt = """
        You are a STRICT retrieval-augmented generation (RAG) assistant.
        
        You MUST follow these rules EXACTLY:
        
        1. You can ONLY use facts directly copied from the context.
        2. You may NOT paraphrase, explain, or infer anything not explicitly written in the context.
        3. If the answer is NOT explicitly present in the context, reply EXACTLY with:
           "Answer not found in the provided documents."
        4. When citing sources, use ONLY the format [Source ID: x].
        5. DO NOT guess.
        6. DO NOT use general knowledge.
        7. DO NOT expand definitions or add wording that is not in the context.
        
        Context:
        %s
        
        Question:
        %s
        
        Now produce your answer using ONLY direct statements from the context.
        If no direct matching text exists, reply with:
        "Answer not found in the provided documents."
        """.formatted(contextBuilder.toString(), req.question);


        // LLM Generation
        String answer = gen.generate(prompt);

        // Extract used source IDs
        Set<Integer> usedSourceIds = extractSourceIds(answer);

        // Build citations only for used IDs
        List<CitationDto> cites = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            if (!usedSourceIds.contains(i + 1)) continue;

            PageChunk c = chunks.get(i);
            CitationDto cd = new CitationDto();
            cd.document_id = c.getDocument().getId();
            cd.file_name = c.getDocument().getOriginalFileName();
            cd.page_number = c.getPageNumber();
            cd.snippet = c.getText().length() > 400
                    ? c.getText().substring(0, 400) + "..."
                    : c.getText();
            cites.add(cd);
        }

        // Optional translation
        if (Boolean.TRUE.equals(req.translate_sources)) {
            for (CitationDto c : cites) {
                c.translated_snippet =
                        gen.generate("Translate to English concisely:\n---\n" + c.snippet);
            }
        }

        // Response
        AskResponseDto resp = new AskResponseDto();
        resp.answer = answer;
        resp.citations = cites;
        return resp;
    }

    // Extract [Source ID: x] references
    private Set<Integer> extractSourceIds(String text) {
        Set<Integer> ids = new HashSet<>();
        Matcher m = Pattern.compile("Source ID:\\s*(\\d+)").matcher(text);
        while (m.find()) {
            ids.add(Integer.parseInt(m.group(1)));
        }
        return ids;
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


