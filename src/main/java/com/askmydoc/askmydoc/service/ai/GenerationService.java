package com.askmydoc.askmydoc.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;

@Service
public class GenerationService {
    @Value("${gemini.apiBase}") String apiBase;
    @Value("${gemini.modelText}") String model;
    @Value("${gemini.apiKey}") String apiKey;

    private final WebClient web = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();

    public String generate(String prompt) {
        Map<String,Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        // Build list of candidate models to try in order.
        List<String> candidates = new ArrayList<>();
        // Base model first
        candidates.add(model);
        // If it ends with a stray hyphen, include trimmed version
        if (model.endsWith("-")) {
            candidates.add(model.substring(0, model.length()-1));
        }
        // Add -latest variant if not already
        if (!model.endsWith("-latest")) {
            candidates.add(model + "-latest");
        }
        // Common numbered variants (avoid duplicates)
        for (String suffix : List.of("-001","-002")) {
            String m = model.endsWith("-") ? model.substring(0, model.length()-1) + suffix : model + suffix;
            if (!candidates.contains(m)) candidates.add(m);
        }

        Map res = null;
        String usedModel = null;
        for (String m : candidates) {
            String url = apiBase + "/" + m + ":generateContent"; // header holds key
            try {
                res = web.post().uri(url)
                        .header("x-goog-api-key", apiKey)
                        .bodyValue(body)
                        .retrieve().bodyToMono(Map.class).block();
                usedModel = m;
                break; // success
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound nf) {
                // try next candidate
            }
        }
        if (res == null) {
            throw new IllegalStateException("Gemini model not found. Tried: " + String.join(", ", candidates));
        }
        return extractText(res) + "\n\n(model: " + usedModel + ")";
    }

    private String extractText(Map res) {
        if (res == null) return "I couldn’t find that in the documents.";
        List<Map> cands = (List<Map>) res.get("candidates");
        if (cands == null || cands.isEmpty()) return "I couldn’t find that in the documents.";
        Map content = (Map) cands.get(0).get("content");
        if (content == null) return "I couldn’t find that in the documents.";
        List<Map> parts = (List<Map>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "I couldn’t find that in the documents.";
        Object text = parts.get(0).get("text");
        return text == null ? "I couldn’t find that in the documents." : text.toString();
    }
}
