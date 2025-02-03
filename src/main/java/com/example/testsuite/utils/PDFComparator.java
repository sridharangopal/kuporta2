package com.example.testsuite.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

@Slf4j
@Component
public class PDFComparator {

    private final TextComparator textComparator;
    private final FontComparator fontComparator;

    @Autowired
    public PDFComparator(TextComparator textComparator, FontComparator fontComparator) {
        this.textComparator = textComparator;
        this.fontComparator = fontComparator;
    }

    /**
     * Compare PDFs and generate comprehensive diff report including visual, text, and font differences
     * @param generatedPdf the generated PDF file
     * @param goldCopyPdf the gold copy PDF file
     * @param outputDir directory to save diff outputs
     * @return ComparisonResult containing all differences found
     * @throws IOException if there's an error processing the PDFs
     */
    public ComparisonResult compare(File generatedPdf, File goldCopyPdf, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir);
        }

        ComparisonResult result = new ComparisonResult();

        // Visual comparison
        File diffImage = new File(outputDir, "visual-diff.png");
        result.setVisuallyIdentical(compareAndGenerateDiff(generatedPdf, goldCopyPdf, diffImage));
        
        // Text comparison
        List<TextComparator.TextDifference> textDiffs = textComparator.compareContent(generatedPdf, goldCopyPdf);
        result.setTextDifferences(textDiffs);
        
        // Font comparison
        List<FontComparator.FontDifference> fontDiffs = fontComparator.compareFonts(generatedPdf, goldCopyPdf);
        result.setFontDifferences(fontDiffs);

        // Generate report
        generateReport(result, new File(outputDir, "comparison-report.txt"));

        return result;
    }

    private boolean compareAndGenerateDiff(File generatedPdf, File goldCopyPdf, File diffOutputFile) throws IOException {
        try (PDDocument generatedDoc = PDDocument.load(generatedPdf);
             PDDocument goldCopyDoc = PDDocument.load(goldCopyPdf)) {

            if (generatedDoc.getNumberOfPages() != goldCopyDoc.getNumberOfPages()) {
                log.warn("PDFs have different number of pages: Generated={}, GoldCopy={}",
                        generatedDoc.getNumberOfPages(), goldCopyDoc.getNumberOfPages());
                return false;
            }

            PDFRenderer generatedRenderer = new PDFRenderer(generatedDoc);
            PDFRenderer goldCopyRenderer = new PDFRenderer(goldCopyDoc);

            boolean differencesFound = false;

            for (int page = 0; page < generatedDoc.getNumberOfPages(); page++) {
                BufferedImage generatedImage = generatedRenderer.renderImageWithDPI(page, 300);
                BufferedImage goldCopyImage = goldCopyRenderer.renderImageWithDPI(page, 300);

                if (!compareImages(generatedImage, goldCopyImage)) {
                    differencesFound = true;
                    BufferedImage diffImage = generateVisualDiff(generatedImage, goldCopyImage);
                    ImageIO.write(diffImage, "PNG", diffOutputFile);
                    break; // Stop at first difference found
                }
            }

            return !differencesFound;
        }
    }

    private boolean compareImages(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }

        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private BufferedImage generateVisualDiff(BufferedImage img1, BufferedImage img2) {
        int width = Math.max(img1.getWidth(), img2.getWidth());
        int height = Math.max(img1.getHeight(), img2.getHeight());

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = diffImage.createGraphics();

        // Draw the first image as the base
        g2d.drawImage(img1, 0, 0, null);

        // Set up for drawing differences
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2d.setColor(Color.RED);

        // Draw only the different pixels in red
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (x < img1.getWidth() && y < img1.getHeight() &&
                    x < img2.getWidth() && y < img2.getHeight() &&
                    img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    g2d.fillRect(x, y, 1, 1);
                }
            }
        }
        g2d.dispose();

        return diffImage;
    }

    private void generateReport(ComparisonResult result, File reportFile) throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("PDF Comparison Report\n")
              .append("===================\n\n");

        // Visual differences
        report.append("Visual Comparison:\n")
              .append(result.isVisuallyIdentical() ? "No visual differences found\n" : "Visual differences found\n")
              .append("\n");

        // Text differences
        report.append("Text Differences:\n");
        if (result.getTextDifferences().isEmpty()) {
            report.append("No text differences found\n");
        } else {
            for (TextComparator.TextDifference diff : result.getTextDifferences()) {
                report.append(diff.toString()).append("\n");
            }
        }
        report.append("\n");

        // Font differences
        report.append("Font Differences:\n");
        if (result.getFontDifferences().isEmpty()) {
            report.append("No font differences found\n");
        } else {
            for (FontComparator.FontDifference diff : result.getFontDifferences()) {
                report.append(diff.toString()).append("\n");
            }
        }

        // Write report to file
        java.nio.file.Files.write(reportFile.toPath(), report.toString().getBytes());
    }

    public static class ComparisonResult {
        private boolean visuallyIdentical;
        private List<TextComparator.TextDifference> textDifferences;
        private List<FontComparator.FontDifference> fontDifferences;

        public boolean isVisuallyIdentical() { return visuallyIdentical; }
        public List<TextComparator.TextDifference> getTextDifferences() { return textDifferences; }
        public List<FontComparator.FontDifference> getFontDifferences() { return fontDifferences; }

        public void setVisuallyIdentical(boolean visuallyIdentical) { 
            this.visuallyIdentical = visuallyIdentical; 
        }
        public void setTextDifferences(List<TextComparator.TextDifference> textDifferences) { 
            this.textDifferences = textDifferences; 
        }
        public void setFontDifferences(List<FontComparator.FontDifference> fontDifferences) { 
            this.fontDifferences = fontDifferences; 
        }

        public boolean hasDifferences() {
            return !visuallyIdentical || 
                   !textDifferences.isEmpty() || 
                   !fontDifferences.isEmpty();
        }
    }
}