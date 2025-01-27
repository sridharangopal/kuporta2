# Document Generation Test Suite

This project provides an end-to-end testing framework for document generation processes, specifically designed to test the flow from XML input to PDF generation through IBM MQ messaging.

## Project Structure

```
src/
├── main/
│   ├── java/com/example/testsuite/
│   │   ├── config/       # Configuration classes
│   │   ├── processor/    # Core business logic
│   │   └── utils/        # Utility classes
│   └── resources/
│       └── application.yml
└── test/
    ├── java/com/example/testsuite/
    │   └── integration/  # Integration tests
    └── resources/
        ├── application-test.yml
        ├── test-inputs/  # XML test files
        └── gold-copies/  # Expected PDF results
```

## Prerequisites

- Java 17
- Maven
- IBM MQ Server
- Running document generation service that:
  - Consumes XML messages from MQ
  - Generates PDFs
  - Exposes REST API for PDF retrieval

## Configuration

1. Main application configuration: `src/main/resources/application.yml`
2. Test-specific configuration: `src/test/resources/application-test.yml`

### IBM MQ Configuration

Update the following properties in application.yml or application-test.yml:

```yaml
ibm:
  mq:
    host: your-mq-host
    port: your-mq-port
    queue-manager: your-queue-manager
    channel: your-channel
    queue: your-queue-name
    username: your-username
    password: your-password
```

### Document Service Configuration

Update the following properties:

```yaml
document:
  api:
    base-url: http://your-service-host
    pdf-endpoint: /your-pdf-endpoint
```

## Test Data

1. Place XML test files in `src/test/resources/test-inputs/`
   - Sample provided: `sample-policy.xml`
   - XML files should include a policy number in format: `<policyNumber>POL-YYYY-XXX</policyNumber>`
2. Place corresponding gold copy PDFs in `src/test/resources/gold-copies/`
   - Name format: `gold_[policyNumber].pdf`
   - Example: `gold_POL-2024-001.pdf`

## Running Tests

Run all tests:
```bash
mvn clean test
```

Run specific test:
```bash
mvn test -Dtest=DocumentGenerationIntegrationTest
```

## Test Results

- Test results are generated in `target/test-output/`
- For each test case:
  - Generated PDF: `generated_[policyNumber].pdf`
  - Visual diff (if differences found): `diff_[policyNumber].png`

## Error Handling

The test suite handles various error scenarios:
- MQ connectivity issues
- Document service unavailability
- PDF generation timeouts
- Invalid XML content
- PDF comparison failures

## Logging

Logging configuration can be adjusted in application-test.yml:

```yaml
logging:
  level:
    com.example.testsuite: DEBUG
    org.springframework.jms: INFO
    com.ibm.mq: INFO
```

## Architecture

### Components

1. **XMLFileProcessor**: Handles loading and parsing of XML test files
2. **DocumentMessageProcessor**: Manages IBM MQ message sending and PDF retrieval
3. **PDFComparator**: Compares generated PDFs with gold copies and generates visual diffs
4. **Shim-PubAPI Tests**: Orchestrates the testing process

### Test Flow

1. Test loads XML file from test-inputs directory
2. Extracts policy number from XML content
3. Sends XML message to IBM MQ queue
4. Polls document service API for generated PDF
5. Compares generated PDF with gold copy
6. Generates visual diff if differences are found
7. Reports test results

## Contributing

When adding new test cases:
1. Add XML file to `test-inputs` directory
2. Add corresponding gold copy PDF to `gold-copies` directory
3. Follow naming conventions for policy numbers
4. Update test documentation if needed