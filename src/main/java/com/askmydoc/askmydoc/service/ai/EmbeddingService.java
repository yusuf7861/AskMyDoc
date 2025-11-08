package com.askmydoc.askmydoc.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;

@Service
public class EmbeddingService {
    @Value("${gemini.apiBase}") String apiBase;
    @Value("${gemini.modelEmbedding}") String model;
    @Value("${gemini.apiKey}") String apiKey;

    private final WebClient web = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public float[] embed(String text) {
        Map<String,Object> body = Map.of(
                "model", model,
                "content", Map.of("parts", List.of(Map.of("text", text)))
        );
        String url = apiBase + "/" + model + ":embedContent"; // use header for API key
        Map res = web.post()
                .uri(url)
                .header("x-goog-api-key", apiKey)
                .bodyValue(body)
                .retrieve().bodyToMono(Map.class).block();

        Map emb = (Map) res.get("embedding");
        List<Double> vals = (List<Double>) emb.get("values");
        float[] v = new float[vals.size()];
        for (int i=0;i<v.length;i++) v[i]=vals.get(i).floatValue();
        return v;
    }

}
