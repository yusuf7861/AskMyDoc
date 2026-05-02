package com.askmydoc.askmydoc.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmbeddingService {
    @Value("${gemini.apiBase}") String apiBase;
    @Value("${gemini.modelEmbedding}") String model;
    @Value("${gemini.apiKey}") String apiKey;

    private final WebClient webClient;

    public float[] embed(String text) {
        Map<String, Object> body = Map.of(
                "model", model,
                "content", Map.of("parts", List.of(Map.of("text", text)))
        );
        String url = apiBase + "/" + model + ":embedContent";
        @SuppressWarnings("unchecked")
        Map<String, Object> res = webClient.post()
                .uri(url)
                .header("x-goog-api-key", apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        @SuppressWarnings("unchecked")
        Map<String, Object> emb = (Map<String, Object>) res.get("embedding");
        @SuppressWarnings("unchecked")
        List<Double> vals = (List<Double>) emb.get("values");
        float[] v = new float[vals.size()];
        for (int i = 0; i < v.length; i++) v[i] = vals.get(i).floatValue();
        return v;
    }
}
