package com.example.testsuite.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TextComparator {

    public enum ComparisonMode {
        LINE_BY_LINE,
        WHOLE_DOCUMENT
    }

    /**
     * Compares text content of two PDFs and returns differences using the specified comparison mode
     * @param generatedPdf the generated PDF file
     * @param goldCopyPdf the gold copy PDF file
     * @param mode the comparison mode to use
     * @param deepDetect when true, considers whitespace differences as changes
     * @return List of text differences found
     * @throws IOException if there's an error processing the PDFs
     */
    public List<TextDifference> compareContent(File generatedPdf, File goldCopyPdf,
            ComparisonMode mode, boolean deepDetect) throws IOException {
        try (PDDocument generatedDoc = PDDocument.load(generatedPdf);
             PDDocument goldCopyDoc = PDDocument.load(goldCopyPdf)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String generatedText = stripper.getText(generatedDoc);
            String goldCopyText = stripper.getText(goldCopyDoc);

            return mode == ComparisonMode.LINE_BY_LINE ?
                findLineByLineDifferences(generatedText, goldCopyText, deepDetect) :
                findDocumentDifferences(generatedText, goldCopyText, deepDetect);
        }
    }

    /**
     * Legacy method for line-by-line comparison
     * @param generatedPdf the generated PDF file
     * @param goldCopyPdf the gold copy PDF file
     * @param deepDetect when true, considers whitespace differences as changes
     * @return List of text differences found
     * @throws IOException if there's an error processing the PDFs
     */
    public List<TextDifference> compareContent(File generatedPdf, File goldCopyPdf, boolean deepDetect) throws IOException {
        try (PDDocument generatedDoc = PDDocument.load(generatedPdf);
             PDDocument goldCopyDoc = PDDocument.load(goldCopyPdf)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String generatedText = stripper.getText(generatedDoc);
            String goldCopyText = stripper.getText(goldCopyDoc);

            return findLineByLineDifferences(generatedText, goldCopyText, deepDetect);
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    public List<TextDifference> compareContent(File generatedPdf, File goldCopyPdf) throws IOException {
        return compareContent(generatedPdf, goldCopyPdf, false);
    }

    private List<TextDifference> findLineByLineDifferences(String text1, String text2, boolean deepDetect) {
        List<TextDifference> differences = new ArrayList<>();
        String[] lines1 = text1.split("\\r?\\n");
        String[] lines2 = text2.split("\\r?\\n");

        int i = 0, j = 0;
        while (i < lines1.length && j < lines2.length) {
            String line1 = deepDetect ? lines1[i] : normalizeWhitespace(lines1[i]);
            String line2 = deepDetect ? lines2[j] : normalizeWhitespace(lines2[j]);

            if (!line1.equals(line2)) {
                differences.add(new TextDifference(
                    i + 1,
                    j + 1,
                    lines1[i],
                    lines2[j],
                    TextDifference.DiffType.MODIFIED
                ));
            }
            i++;
            j++;
        }

        // Handle remaining lines in either document
        while (i < lines1.length) {
            differences.add(new TextDifference(
                i + 1,
                -1,
                lines1[i],
                "",
                TextDifference.DiffType.ADDED
            ));
            i++;
        }

        while (j < lines2.length) {
            differences.add(new TextDifference(
                -1,
                j + 1,
                "",
                lines2[j],
                TextDifference.DiffType.DELETED
            ));
            j++;
        }

        return differences;
    }

    private List<TextDifference> findDocumentDifferences(String text1, String text2, boolean deepDetect) {
        String doc1 = deepDetect ? text1 : normalizeWhitespace(text1);
        String doc2 = deepDetect ? text2 : normalizeWhitespace(text2);

        List<TextDifference> differences = new ArrayList<>();
        if (!doc1.equals(doc2)) {
            // Find the first differing character position
            int pos = 0;
            while (pos < doc1.length() && pos < doc2.length() && doc1.charAt(pos) == doc2.charAt(pos)) {
                pos++;
            }

            // Extract context around the difference
            int contextStart = Math.max(0, pos - 50);
            int contextEnd1 = Math.min(doc1.length(), pos + 50);
            int contextEnd2 = Math.min(doc2.length(), pos + 50);

            String context1 = doc1.substring(contextStart, contextEnd1);
            String context2 = doc2.substring(contextStart, contextEnd2);

            differences.add(new TextDifference(
                0, // Use 0 for whole document comparison
                0,
                context1,
                context2,
                TextDifference.DiffType.MODIFIED
            ));
        }
        return differences;
    }

    /**
     * Normalizes whitespace in a string by:
     * 1. Trimming leading/trailing whitespace
     * 2. Replacing multiple spaces between words with a single space
     */
    private String normalizeWhitespace(String text) {
        return text.trim().replaceAll("\\s+", " ");
    }

    public static class TextDifference {
        public enum DiffType {
            ADDED, DELETED, MODIFIED
        }

        private final int generatedLine;
        private final int goldCopyLine;
        private final String generatedText;
        private final String goldCopyText;
        private final DiffType type;

        public TextDifference(int generatedLine, int goldCopyLine, String generatedText, 
                            String goldCopyText, DiffType type) {
            this.generatedLine = generatedLine;
            this.goldCopyLine = goldCopyLine;
            this.generatedText = generatedText;
            this.goldCopyText = goldCopyText;
            this.type = type;
        }

        public int getGeneratedLine() { return generatedLine; }
        public int getGoldCopyLine() { return goldCopyLine; }
        public String getGeneratedText() { return generatedText; }
        public String getGoldCopyText() { return goldCopyText; }
        public DiffType getType() { return type; }

        @Override
        public String toString() {
            switch (type) {
                case ADDED:
                    return String.format("Line %d: Added \"%s\"", generatedLine, generatedText);
                case DELETED:
                    return String.format("Line %d: Deleted \"%s\"", goldCopyLine, goldCopyText);
                case MODIFIED:
                    return String.format("Line %d vs %d: Changed from \"%s\" to \"%s\"",
                            goldCopyLine, generatedLine, goldCopyText, generatedText);
                default:
                    return "Unknown difference type";
            }
        }
    }
}