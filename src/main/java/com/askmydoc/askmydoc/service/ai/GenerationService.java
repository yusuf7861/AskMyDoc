package com.askmydoc.askmydoc.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GenerationService {
    @Value("${gemini.apiBase}") String apiBase;
    @Value("${gemini.modelText}") String model;
    @Value("${gemini.apiKey}") String apiKey;

    private final WebClient webClient;

    public String generate(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.0)
        );

        // Try the configured model first, then fall back to common variants.
        List<String> candidates = buildCandidates(model);

        Map<String, Object> res = null;
        String usedModel = null;
        for (String m : candidates) {
            String url = apiBase + "/" + m + ":generateContent";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = webClient.post().uri(url)
                        .header("x-goog-api-key", apiKey)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                res = response;
                usedModel = m;
                break;
            } catch (WebClientResponseException.NotFound ignored) {
                // try next candidate
            }
        }
        if (res == null) {
            throw new IllegalStateException("Gemini model not found. Tried: " + String.join(", ", candidates));
        }
        return extractText(res) + "\n\n(model: " + usedModel + ")";
    }

    private List<String> buildCandidates(String base) {
        List<String> candidates = new ArrayList<>();
        candidates.add(base);
        String trimmed = base.endsWith("-") ? base.substring(0, base.length() - 1) : base;
        if (!trimmed.equals(base)) candidates.add(trimmed);
        if (!base.endsWith("-latest")) candidates.add(trimmed + "-latest");
        for (String suffix : List.of("-001", "-002")) {
            String m = trimmed + suffix;
            if (!candidates.contains(m)) candidates.add(m);
        }
        return candidates;
    }

    private String extractText(Map<String, Object> res) {
        if (res == null) return "I couldn't find that in the documents.";
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cands = (List<Map<String, Object>>) res.get("candidates");
        if (cands == null || cands.isEmpty()) return "I couldn't find that in the documents.";
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) cands.get(0).get("content");
        if (content == null) return "I couldn't find that in the documents.";
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) return "I couldn't find that in the documents.";
        Object text = parts.get(0).get("text");
        return text == null ? "I couldn't find that in the documents." : text.toString();
    }
}
