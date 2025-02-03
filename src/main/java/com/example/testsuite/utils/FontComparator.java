package com.example.testsuite.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FontComparator extends PDFTextStripper {

    private final List<FontDifference> differences = new ArrayList<>();
    private Map<String, FontInfo> fontInfoMap = new HashMap<>();
    public FontComparator() throws IOException {
        super();
    }

    /**
     * Compares fonts used in two PDFs and returns differences
     * @param generatedPdf the generated PDF file
     * @param goldCopyPdf the gold copy PDF file
     * @return List of font differences found
     * @throws IOException if there's an error processing the PDFs
     */
    public List<FontDifference> compareFonts(File generatedPdf, File goldCopyPdf) throws IOException {
        differences.clear();
        fontInfoMap.clear();
        
        Map<String, FontInfo> generatedFontMap = extractFontInfo(generatedPdf, true);
        Map<String, FontInfo> goldCopyFontMap = extractFontInfo(goldCopyPdf, false);

        // Compare font usage
        for (Map.Entry<String, FontInfo> entry : generatedFontMap.entrySet()) {
            String text = entry.getKey();
            FontInfo generatedFont = entry.getValue();
            FontInfo goldCopyFont = goldCopyFontMap.get(text);

            if (goldCopyFont == null) {
                differences.add(new FontDifference(text, generatedFont, null));
            } else if (!generatedFont.equals(goldCopyFont)) {
                differences.add(new FontDifference(text, generatedFont, goldCopyFont));
            }
        }

        // Check for text that only appears in gold copy
        for (Map.Entry<String, FontInfo> entry : goldCopyFontMap.entrySet()) {
            String text = entry.getKey();
            if (!generatedFontMap.containsKey(text)) {
                differences.add(new FontDifference(text, null, entry.getValue()));
            }
        }

        return differences;
    }

    private Map<String, FontInfo> extractFontInfo(File pdfFile, boolean isGenerated) throws IOException {
        fontInfoMap.clear();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            for (PDPage page : document.getPages()) {
                processPage(page);
            }
        }
        return new HashMap<>(fontInfoMap);
    }

    @Override
    protected void processTextPosition(TextPosition text) {
        PDFont font = text.getFont();
        String textContent = text.getUnicode();
        
        // Create font info for the current text
        FontInfo fontInfo = new FontInfo(
            font.getName(),
            text.getFontSizeInPt(),
            getFontWeight(font),
            isItalicFont(font)
        );

        // Store or update font information for this text
        fontInfoMap.put(textContent, fontInfo);
    }

    private float getFontWeight(PDFont font) {
        try {
            return font.getFontDescriptor() != null ? 
                   font.getFontDescriptor().getFontWeight() : 
                   400f; // Default weight if not specified
        } catch (Exception e) {
            return 400f;
        }
    }

    private boolean isItalicFont(PDFont font) {
        try {
            return font.getFontDescriptor() != null && 
                   font.getFontDescriptor().isItalic();
        } catch (Exception e) {
            return false;
        }
    }

    public static class FontInfo {
        private final String fontName;
        private final float fontSize;
        private final float fontWeight;
        private final boolean isItalic;

        public FontInfo(String fontName, float fontSize, float fontWeight, boolean isItalic) {
            this.fontName = fontName;
            this.fontSize = fontSize;
            this.fontWeight = fontWeight;
            this.isItalic = isItalic;
        }

        public String getFontName() { return fontName; }
        public float getFontSize() { return fontSize; }
        public float getFontWeight() { return fontWeight; }
        public boolean isItalic() { return isItalic; }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FontInfo)) return false;
            FontInfo other = (FontInfo) obj;
            return fontName.equals(other.fontName) &&
                   Math.abs(fontSize - other.fontSize) < 0.1 &&
                   Math.abs(fontWeight - other.fontWeight) < 0.1 &&
                   isItalic == other.isItalic;
        }

        @Override
        public int hashCode() {
            int result = fontName.hashCode();
            result = 31 * result + Float.floatToIntBits(fontSize);
            result = 31 * result + Float.floatToIntBits(fontWeight);
            result = 31 * result + (isItalic ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return String.format("%s %.1fpt%s%s",
                    fontName,
                    fontSize,
                    fontWeight >= 700 ? " bold" : "",
                    isItalic ? " italic" : "");
        }
    }

    public static class FontDifference {
        private final String text;
        private final FontInfo generatedFont;
        private final FontInfo goldCopyFont;

        public FontDifference(String text, FontInfo generatedFont, FontInfo goldCopyFont) {
            this.text = text;
            this.generatedFont = generatedFont;
            this.goldCopyFont = goldCopyFont;
        }

        public String getText() { return text; }
        public FontInfo getGeneratedFont() { return generatedFont; }
        public FontInfo getGoldCopyFont() { return goldCopyFont; }

        @Override
        public String toString() {
            if (goldCopyFont == null) {
                return String.format("Text \"%s\" appears only in generated PDF with font: %s",
                        text, generatedFont);
            } else if (generatedFont == null) {
                return String.format("Text \"%s\" appears only in gold copy with font: %s",
                        text, goldCopyFont);
            } else {
                return String.format("Text \"%s\" has different fonts - Generated: %s, Gold Copy: %s",
                        text, generatedFont, goldCopyFont);
            }
        }
    }
}