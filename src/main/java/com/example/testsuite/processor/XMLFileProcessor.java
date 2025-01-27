package com.example.testsuite.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class XMLFileProcessor {

    @Value("${document.input.xml-folder}")
    private String xmlFolderPath;

    /**
     * Loads all XML files from the configured input folder
     * @return List of XML files found in the folder
     * @throws IOException if there's an error accessing the folder
     */
    public List<File> loadXMLFiles() throws IOException {
        Path folderPath = Paths.get(xmlFolderPath);
        if (!Files.exists(folderPath)) {
            throw new IOException("XML folder does not exist: " + xmlFolderPath);
        }

        try (Stream<Path> paths = Files.walk(folderPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".xml"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Reads the content of an XML file
     * @param file the XML file to read
     * @return the content of the file as a String
     * @throws IOException if there's an error reading the file
     */
    public String readXMLContent(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getPath());
        }
        return Files.readString(file.toPath());
    }

    /**
     * Extracts the policy number from the XML content using the specified XPath
     * This is a placeholder implementation - modify according to actual XML structure
     * @param xmlContent the XML content to parse
     * @return the extracted policy number
     */
    public String extractPolicyNumber(String xmlContent) {
        // TODO: Implement actual XML parsing logic based on the XML structure
        // This is just a placeholder implementation
        int startIndex = xmlContent.indexOf("<policyNumber>");
        int endIndex = xmlContent.indexOf("</policyNumber>");
        if (startIndex != -1 && endIndex != -1) {
            return xmlContent.substring(startIndex + 13, endIndex);
        }
        throw new IllegalArgumentException("Could not find policy number in XML content");
    }
}