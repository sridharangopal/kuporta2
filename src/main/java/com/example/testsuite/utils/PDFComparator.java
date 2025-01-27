package com.example.testsuite.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

@Slf4j
@Component
public class PDFComparator {

    /**
     * Compares two PDFs and generates a visual diff
     * @param generatedPdf the generated PDF file
     * @param goldCopyPdf the gold copy PDF file
     * @param diffOutputFile the file to save the diff image to
     * @return true if PDFs are identical, false if differences are found
     * @throws IOException if there's an error processing the PDFs
     */
    public boolean compareAndGenerateDiff(File generatedPdf, File goldCopyPdf, File diffOutputFile) throws IOException {
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

            if (differencesFound) {
                log.info("Differences found. Visual diff saved to: {}", diffOutputFile.getAbsolutePath());
                return false;
            }

            log.info("PDFs are identical");
            return true;
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

        // Draw the first image
        g2d.drawImage(img1, 0, 0, null);

        // Set composite mode to highlight differences
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2d.setColor(Color.RED);

        // Draw the second image with transparency to show differences
        g2d.drawImage(img2, 0, 0, null);
        g2d.dispose();

        return diffImage;
    }
}