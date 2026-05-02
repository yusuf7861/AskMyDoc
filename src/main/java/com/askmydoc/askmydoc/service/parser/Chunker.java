package com.askmydoc.askmydoc.service.parser;

import org.springframework.stereotype.Component;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;

@Component
public class Chunker {

    /**
     * Splits {@code text} into chunks of at most {@code maxChars} characters.
     * When a chunk boundary is hit, the last {@code overlap} characters of the
     * previous chunk are prepended to the next chunk so that context is not lost
     * across boundaries.  Split points respect sentence boundaries where possible.
     */
    public List<String> split(String text, int maxChars, int overlap) {
        List<String> out = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");

        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            int extra = current.length() > 0 ? 1 : 0; // space separator
            if (current.length() > 0 && current.length() + extra + sentence.length() > maxChars) {
                String chunk = current.toString();
                out.add(chunk);
                current = new StringBuilder();
                if (overlap > 0) {
                    int start = Math.max(0, chunk.length() - overlap);
                    current.append(chunk, start, chunk.length()).append(" ");
                }
            }
            if (current.length() > 0 && current.charAt(current.length() - 1) != ' ') {
                current.append(' ');
            }
            current.append(sentence);
        }
        if (current.length() > 0) out.add(current.toString());
        return out;
    }

    /** Serialises a float array to raw bytes (LITTLE_ENDIAN IEEE-754). */
    public byte[] toBytes(float[] v) {
        ByteBuffer bb = ByteBuffer.allocate(v.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : v) bb.putFloat(f);
        return bb.array();
    }

    /** Deserialises a byte array produced by {@link #toBytes(float[])} back to floats. */
    public float[] fromBytes(byte[] b) {
        FloatBuffer fb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        float[] v = new float[fb.remaining()];
        fb.get(v);
        return v;
    }
}
