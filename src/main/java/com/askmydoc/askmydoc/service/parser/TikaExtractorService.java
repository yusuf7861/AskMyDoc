package com.askmydoc.askmydoc.service.parser;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import java.io.*;

@Service
public class TikaExtractorService {
    private final Tika tika = new Tika();
    public String extractText(InputStream in) throws IOException, TikaException { return tika.parseToString(in); }
    public String detect(InputStream in, String name) throws IOException { return tika.detect(in, name); }
}

