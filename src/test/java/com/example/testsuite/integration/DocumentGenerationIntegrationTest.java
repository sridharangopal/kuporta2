package com.example.testsuite.integration;

import com.example.testsuite.processor.DocumentMessageProcessor;
import com.example.testsuite.processor.XMLFileProcessor;
import com.example.testsuite.utils.PDFComparator;
import com.example.testsuite.utils.PDFComparator.ComparisonResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class DocumentGenerationIntegrationTest {

    @Autowired
    private XMLFileProcessor xmlFileProcessor;

    @Autowired
    private DocumentMessageProcessor messageProcessor;

    @Autowired
    private PDFComparator pdfComparator;

    @Value("${document.input.xml-folder}")
    private String xmlFolderPath;

    @Value("${document.input.gold-copies-folder}")
    private String goldCopiesFolderPath;

    private Path testOutputPath;

    @BeforeEach
    void setUp(TestInfo testInfo) throws IOException {
        // Create test output directory if it doesn't exist
        testOutputPath = Paths.get("target", "test-output", 
            testInfo.getDisplayName().replaceAll("[^a-zA-Z0-9.-]", "_"));
        Files.createDirectories(testOutputPath);
    }

    static Stream<File> xmlTestFiles() throws IOException {
        Path xmlFolder = Paths.get(System.getProperty("user.dir"), "src/test/resources/test-inputs");
        return Files.walk(xmlFolder)
                .filter(path -> path.toString().toLowerCase().endsWith(".xml"))
                .map(Path::toFile);
    }

    @ParameterizedTest(name = "Test document generation for {0}")
    @MethodSource("xmlTestFiles")
    void testDocumentGeneration(File xmlFile) throws Exception {
        log.info("Testing document generation for file: {}", xmlFile.getName());

        // Read XML content
        String xmlContent = xmlFileProcessor.readXMLContent(xmlFile);
        
        // Extract policy number for identifying related files
        String policyNumber = xmlFileProcessor.extractPolicyNumber(xmlContent);
        
        // Send XML to MQ
        messageProcessor.sendToQueue(xmlContent);
        
        // Wait for and retrieve the generated PDF
        byte[] pdfContent = messageProcessor.retrievePDF(policyNumber);
        
        // Save the retrieved PDF
        File generatedPdfFile = testOutputPath.resolve("generated_" + policyNumber + ".pdf").toFile();
        Files.write(generatedPdfFile.toPath(), pdfContent);
        
        // Get the gold copy PDF
        File goldCopyFile = Paths.get(goldCopiesFolderPath, "gold_" + policyNumber + ".pdf").toFile();
        
        // Create comparison output directory
        Path comparisonPath = testOutputPath.resolve("comparison_" + policyNumber);
        Files.createDirectories(comparisonPath);
        
        // Compare PDFs and get results
        ComparisonResult result = pdfComparator.compare(generatedPdfFile, goldCopyFile, comparisonPath.toFile());
        
        // Log any differences found
        if (result.hasDifferences()) {
            log.error("Differences found in generated PDF for policy {}:", policyNumber);
            if (!result.isVisuallyIdentical()) {
                log.error("- Visual differences detected (see: {})", 
                    comparisonPath.resolve("visual-diff.png"));
            }
            if (!result.getTextDifferences().isEmpty()) {
                log.error("- {} text differences found", result.getTextDifferences().size());
                result.getTextDifferences().forEach(diff -> log.error("  * {}", diff));
            }
            if (!result.getFontDifferences().isEmpty()) {
                log.error("- {} font differences found", result.getFontDifferences().size());
                result.getFontDifferences().forEach(diff -> log.error("  * {}", diff));
            }
        }
        
        // Assert no differences were found
        assertFalse(result.hasDifferences(), 
            String.format("Generated PDF does not match gold copy for policy %s. See detailed report at: %s", 
                policyNumber, comparisonPath.resolve("comparison-report.txt")));
    }
}