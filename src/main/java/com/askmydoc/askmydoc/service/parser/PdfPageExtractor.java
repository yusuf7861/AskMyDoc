package com.askmydoc.askmydoc.service.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import java.io.*;

@Service
public class PdfPageExtractor {
    public int getPageCount(InputStream in) throws IOException {
        try (PDDocument pdf = PDDocument.load(in)) { return pdf.getNumberOfPages(); }
    }
    public String extractPage(InputStream in, int pageNumber) throws IOException {
        try (PDDocument pdf = PDDocument.load(in)) {
            PDFTextStripper s = new PDFTextStripper();
            s.setStartPage(pageNumber); s.setEndPage(pageNumber);
            return s.getText(pdf);
        }
    }
}

