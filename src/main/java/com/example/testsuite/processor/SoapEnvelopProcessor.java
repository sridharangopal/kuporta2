package com.example.testsuite.processor;

import org.springframework.stereotype.Component;

@Component
public class SoapEnvelopProcessor {

    private static final String SOAP_ENVELOPE_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope 
                xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:for="http://www.example.com/FormsService">
                <soapenv:Header/>
                <soapenv:Body>
                    <for:submitRequest>
                        <formsRequest>
                            <LOB>%s</LOB>
                            <TargetEnvironment>%s</TargetEnvironment>
                            <EmailAddress>%s</EmailAddress>
                            <GetAFPPDF>false</GetAFPPDF>
                            <Request>
                                %s
                            </Request>
                        </formsRequest>
                    </for:submitRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """;

    /**
     * Creates a SOAP envelope with the provided parameters and XML content
     * @param lob Line of Business value
     * @param targetEnvironment Target environment value
     * @param emailAddress Email address
     * @param getAFPPDF Whether to get AFP PDF
     * @param xmlContent The XML content to be wrapped in the Request tag
     * @return Complete SOAP message
     */
    public String createSoapEnvelope(String lob, String targetEnvironment, 
            String emailAddress, String xmlContent) {
        
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("XML content cannot be null or empty");
        }

        // Remove XML declaration if present to avoid multiple declarations
        String cleanedXml = xmlContent.replaceFirst("<\\?xml.*\\?>", "").trim();
        
        return String.format(SOAP_ENVELOPE_TEMPLATE, 
                lob, targetEnvironment, emailAddress, cleanedXml);
    }
}
