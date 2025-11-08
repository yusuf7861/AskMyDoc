package com.askmydoc.askmydoc.service.ai;

import com.askmydoc.askmydoc.model.PageChunk;
import com.askmydoc.askmydoc.repository.PageChunkRepository;
import com.askmydoc.askmydoc.service.parser.Chunker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RetrievalService {
    private final PageChunkRepository chunkRepo;
    private final EmbeddingService embeddingService;
    private final Chunker chunker;

    public List<PageChunk> topK(String question, List<Long> docIds, int k) {
        float[] q = embeddingService.embed(question);
        List<PageChunk> pool = (docIds==null || docIds.isEmpty())
                ? chunkRepo.findAll()
                : chunkRepo.findByDocumentIdIn(docIds);

        record Scored(PageChunk c, double s){}
        return pool.stream().map(c -> {
                    float[] v = chunker.fromCsv(c.getEmbeddingJson());
                    return new Scored(c, cosine(q, v));
                }).sorted((a,b) -> Double.compare(b.s, a.s))
                .limit(Math.max(1,k)).map(Scored::c).toList();
    }

    private double cosine(float[] a, float[] b) {
        double dot=0,na=0,nb=0;
        for (int i=0;i<a.length;i++){ dot+=a[i]*b[i]; na+=a[i]*a[i]; nb+=b[i]*b[i]; }
        return dot/(Math.sqrt(na)*Math.sqrt(nb)+1e-9);
    }
}
