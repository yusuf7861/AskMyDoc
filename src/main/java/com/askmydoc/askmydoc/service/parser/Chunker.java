package com.askmydoc.askmydoc.service.parser;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class Chunker {
    // Improved idea for Chunker.java
    public List<String> split(String text, int maxChars, int overlap) {
        List<String> out = new ArrayList<>();
        // Regex to split by sentence boundaries while keeping punctuation
        String[] sentences = text.split("(?<=[.!?])\\s+");

        StringBuilder currentChunk = new StringBuilder();
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() + (currentChunk.length() > 0 ? 1 : 0) > maxChars) {
                out.add(currentChunk.toString());
                // Implement overlap logic here if needed, or simple sliding window
                currentChunk = new StringBuilder(sentence);
            } else {
                if (currentChunk.length() > 0) currentChunk.append(" ");
                currentChunk.append(sentence);
            }
        }
        if (currentChunk.length() > 0) out.add(currentChunk.toString());
        return out;
    }
    public String toCsv(float[] v) {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<v.length;i++){ if(i>0) sb.append(','); sb.append(v[i]); }
        return sb.toString();
    }
    public float[] fromCsv(String csv) {
        String[] p = csv.split(","); float[] v = new float[p.length];
        for (int i=0;i<p.length;i++) v[i]=Float.parseFloat(p[i]); return v;
    }
}
