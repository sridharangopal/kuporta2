package com.example.testsuite.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    @Disabled
    public void testPDFComparator() throws IOException {
        // Get the before copy PDF
        File beforeFile = Paths.get("src/test/resources/pdfs/Before.pdf").toFile();
        // Get the gold copy PDF
        File afterFile = Paths.get("src/test/resources/pdfs/After.pdf").toFile();
        // Generate diff file path
        File diffFile = testOutputPath.resolve("diff.png").toFile();
        // Compare PDFs and assert
        boolean isMatch = pdfComparator.compareAndGenerateDiff(afterFile, beforeFile, diffFile);

        assertTrue(isMatch,
                String.format("Generated PDF does not match before copy. See diff: %s", diffFile.getAbsolutePath()));
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
        
        File diffFile = comparisonPath.resolve("diff.png").toFile();
        
        // Compare PDFs and assert
        boolean isMatch = pdfComparator.compareAndGenerateDiff(
            proofFile.toFile(),
            mockupFile.toFile(),
            diffFile
        );

        assertTrue(isMatch,
            String.format("PDF %s does not match proof copy. See diff: %s", 
                pdfFileName, diffFile.getAbsolutePath()));
    }
}
