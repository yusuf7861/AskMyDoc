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
        Map<String,Object> body = Map.of("contents",
                List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );
        Map res = web.post()
                .uri(apiBase + "/models/" + model + ":generateContent?key=" + apiKey)
                .bodyValue(body).retrieve().bodyToMono(Map.class).block();

        List<Map> cands = (List<Map>) res.get("candidates");
        Map content = (Map) cands.get(0).get("content");
        List<Map> parts = (List<Map>) content.get("parts");
        return (String) parts.get(0).get("text");
    }
}
