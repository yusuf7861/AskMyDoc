package com.askmydoc.askmydoc.controller;

import com.askmydoc.askmydoc.service.ai.EmbeddingService;
import com.askmydoc.askmydoc.service.ai.GenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GeminiTestController {

    private final GenerationService generationService;
    private final EmbeddingService embeddingService;

    @GetMapping("/api/v1/test/gemini")
    public String testGemini() {
        String response = generationService.generate("Hello Gemini, are you connected?");
        return "Gemini says: " + response;
    }

    @GetMapping("/api/v1/test/embed")
    public String testEmbedding() {
        float[] vector = embeddingService.embed("This is a test input for AskMyDoc");
        return "✅ Embedding vector length: " + vector.length;
    }

}
