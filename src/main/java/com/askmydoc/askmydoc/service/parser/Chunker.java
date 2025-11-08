package com.askmydoc.askmydoc.service.parser;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class Chunker {
    public List<String> split(String text, int maxChars, int overlap) {
        List<String> out = new ArrayList<>(); int n = text.length();
        for (int start=0; start<n; start+=(maxChars-overlap)) {
            int end = Math.min(n, start+maxChars); out.add(text.substring(start,end));
            if (end==n) break;
        }
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
