package com.example.testsuite.integration;

import com.example.testsuite.processor.DocumentMessageProcessor;
import com.example.testsuite.processor.XMLFileProcessor;
import com.example.testsuite.utils.PDFComparator;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

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
        
        // Generate diff file path
        File diffFile = testOutputPath.resolve("diff_" + policyNumber + ".png").toFile();
        
        // Compare PDFs and assert
        boolean isMatch = pdfComparator.compareAndGenerateDiff(generatedPdfFile, goldCopyFile, diffFile);
        
        assertTrue(isMatch, 
            String.format("Generated PDF does not match gold copy for policy %s. See diff: %s", 
                policyNumber, diffFile.getAbsolutePath()));
    }
}