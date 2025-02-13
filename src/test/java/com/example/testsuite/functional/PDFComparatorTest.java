package com.example.testsuite.functional;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.testsuite.utils.PDFComparator;
import com.example.testsuite.utils.PDFComparator.ComparisonResult;
import com.example.testsuite.utils.TextComparator;
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

    @ParameterizedTest(name = "Compare PDF Line by Line: {0}")
    @MethodSource("pdfFileNames")
    void testPDFPairComparisonLineByLine(String pdfFileName) throws IOException {
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

    @ParameterizedTest(name = "Compare PDF Whole Document: {0}")
    @MethodSource("pdfFileNames")
    void testPDFPairComparisonWholeDocument(String pdfFileName) throws IOException {
        // Get the mockup and proof files
        Path mockupFile = Paths.get("src/test/resources/pdfs/Mockup", pdfFileName);
        Path proofFile = Paths.get("src/test/resources/pdfs/Proofs", pdfFileName);

        // Ensure both files exist
        assertTrue(Files.exists(mockupFile), "Mockup file not found: " + mockupFile);
        assertTrue(Files.exists(proofFile), "Proof file not found: " + proofFile);

        // Create a subdirectory for this comparison
        Path comparisonPath = testOutputPath.resolve(pdfFileName.replace(".pdf", "") + "_whole");
        Files.createDirectories(comparisonPath);
        
        // Compare PDFs using whole document mode
        ComparisonResult result = pdfComparator.compare(
            proofFile.toFile(),
            mockupFile.toFile(),
            comparisonPath.toFile(),
            TextComparator.ComparisonMode.WHOLE_DOCUMENT,
            false
        );

        // Log and assert results
        logComparisonResults(pdfFileName, result);
        assertFalse(result.hasDifferences(),
            String.format("PDF %s has differences with proof copy (whole document mode). See report at: %s",
                pdfFileName, comparisonPath.resolve("comparison-report.txt")));
    }

    @ParameterizedTest(name = "Compare PDF with Deep Detection: {0}")
    @MethodSource("pdfFileNames")
    void testPDFPairComparisonWithDeepDetection(String pdfFileName) throws IOException {
        // Get the mockup and proof files
        Path mockupFile = Paths.get("src/test/resources/pdfs/Mockup", pdfFileName);
        Path proofFile = Paths.get("src/test/resources/pdfs/Proofs", pdfFileName);

        // Ensure both files exist
        assertTrue(Files.exists(mockupFile), "Mockup file not found: " + mockupFile);
        assertTrue(Files.exists(proofFile), "Proof file not found: " + proofFile);

        // Create subdirectories for both comparison modes
        Path lineByLinePath = testOutputPath.resolve(pdfFileName.replace(".pdf", "") + "_deep_line");
        Path wholeDocPath = testOutputPath.resolve(pdfFileName.replace(".pdf", "") + "_deep_whole");
        Files.createDirectories(lineByLinePath);
        Files.createDirectories(wholeDocPath);
        
        // Compare PDFs with deep detection in both modes
        ComparisonResult lineResult = pdfComparator.compare(
            proofFile.toFile(),
            mockupFile.toFile(),
            lineByLinePath.toFile(),
            TextComparator.ComparisonMode.LINE_BY_LINE,
            true
        );

        ComparisonResult wholeResult = pdfComparator.compare(
            proofFile.toFile(),
            mockupFile.toFile(),
            wholeDocPath.toFile(),
            TextComparator.ComparisonMode.WHOLE_DOCUMENT,
            true
        );

        // Log and assert results
        logComparisonResults(pdfFileName + " (deep line-by-line)", lineResult);
        logComparisonResults(pdfFileName + " (deep whole document)", wholeResult);
        
        assertFalse(lineResult.hasDifferences(),
            String.format("PDF %s has differences with proof copy (deep line-by-line). See report at: %s",
                pdfFileName, lineByLinePath.resolve("comparison-report.txt")));
        assertFalse(wholeResult.hasDifferences(),
            String.format("PDF %s has differences with proof copy (deep whole document). See report at: %s",
                pdfFileName, wholeDocPath.resolve("comparison-report.txt")));
    }

    private void logComparisonResults(String pdfFileName, ComparisonResult result) {
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
    }
}
