package com.example.testsuite.functional;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.testsuite.utils.PDFComparator;
import com.example.testsuite.utils.PDFComparator.ComparisonResult;
import com.example.testsuite.utils.TextComparator;
import com.example.testsuite.utils.FontComparator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class PDFComparatorTest {

    @Autowired
    private PDFComparator pdfComparator;

    private Path testOutputPath;

    @BeforeEach
    void setUp(TestInfo testInfo) throws IOException {
        // Create test output directory if it doesn't exist
        testOutputPath = Paths.get("target", "test-output",
                testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9.-]", "_"));
        Files.createDirectories(testOutputPath);
    }

    @Test
    void testComprehensivePDFComparison() throws IOException {
        // Get the test PDF files
        File beforeFile = Paths.get("src/test/resources/pdfs/Before.pdf").toFile();
        File afterFile = Paths.get("src/test/resources/pdfs/After.pdf").toFile();
        
        // Compare PDFs and get comprehensive results
        ComparisonResult result = pdfComparator.compare(beforeFile, afterFile, testOutputPath.toFile());
        
        // Assert the results
        assertAll(
            () -> assertTrue(result.isVisuallyIdentical(), 
                "Visual differences found. Check diff at: " + testOutputPath.resolve("visual-diff.png")),
            () -> assertTrue(result.getTextDifferences().isEmpty(),
                "Text differences found. Check report at: " + testOutputPath.resolve("comparison-report.txt")),
            () -> assertTrue(result.getFontDifferences().isEmpty(),
                "Font differences found. Check report at: " + testOutputPath.resolve("comparison-report.txt"))
        );
    }

    @Test
    void testPDFComparisonWithDifferences() throws IOException {
        // Create test files with known differences
        File file1 = testOutputPath.resolve("test1.pdf").toFile();
        File file2 = testOutputPath.resolve("test2.pdf").toFile();
        
        // Copy test files from resources
        Files.copy(
            Paths.get("src/test/resources/pdfs/Mockup/AS 2112 03 16.pdf"), 
            file1.toPath()
        );
        Files.copy(
            Paths.get("src/test/resources/pdfs/Proofs/AS 2112 03 16.pdf"), 
            file2.toPath()
        );

        // Compare PDFs
        ComparisonResult result = pdfComparator.compare(file1, file2, testOutputPath.toFile());

        // Verify that differences are detected
        assertTrue(result.hasDifferences(), 
            "Expected to find differences between the PDFs");

        // If differences were found, verify the output files exist
        assertTrue(Files.exists(testOutputPath.resolve("visual-diff.png")),
            "Visual diff file should be generated when differences are found");
        assertTrue(Files.exists(testOutputPath.resolve("comparison-report.txt")),
            "Comparison report should be generated");
    }

    static Stream<String> pdfFileNames() throws IOException {
        Path mockupPath = Paths.get("src/test/resources/pdfs/Mockup");
        try (Stream<Path> paths = Files.walk(mockupPath, 1)) {
            return paths
                .filter(path -> !Files.isDirectory(path))
                .map(path -> path.getFileName().toString())
                .filter(name -> name.toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList())
                .stream();
        }
    }

    @ParameterizedTest(name = "Compare PDF: {0}")
    @MethodSource("pdfFileNames")
    void testPDFPairComparison(String pdfFileName) throws IOException {
        // Get the mockup and proof files
        Path mockupFile = Paths.get("src/test/resources/pdfs/Mockup", pdfFileName);
        Path proofFile = Paths.get("src/test/resources/pdfs/Proofs", pdfFileName);

        // Ensure both files exist
        assertTrue(Files.exists(mockupFile), "Mockup file not found: " + mockupFile);
        assertTrue(Files.exists(proofFile), "Proof file not found: " + proofFile);

        // Create a subdirectory for this comparison
        Path comparisonPath = testOutputPath.resolve(pdfFileName.replace(".pdf", ""));
        Files.createDirectories(comparisonPath);
        
        // Compare PDFs and get results
        ComparisonResult result = pdfComparator.compare(
            proofFile.toFile(),
            mockupFile.toFile(),
            comparisonPath.toFile()
        );

        // Log the results
        if (result.hasDifferences()) {
            log.info("Differences found in {}:", pdfFileName);
            if (!result.isVisuallyIdentical()) {
                log.info("- Visual differences detected");
            }
            if (!result.getTextDifferences().isEmpty()) {
                log.info("- {} text differences found", result.getTextDifferences().size());
            }
            if (!result.getFontDifferences().isEmpty()) {
                log.info("- {} font differences found", result.getFontDifferences().size());
            }
        }

        // Assert the comparison
        assertFalse(result.hasDifferences(),
            String.format("PDF %s has differences with proof copy. See report at: %s", 
                pdfFileName, comparisonPath.resolve("comparison-report.txt")));
    }
}
