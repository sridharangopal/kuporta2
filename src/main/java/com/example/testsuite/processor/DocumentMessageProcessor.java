package com.example.testsuite.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.client.core.WebServiceTemplate;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentMessageProcessor {

    private final JmsTemplate jmsTemplate;
    private final RestTemplate restTemplate;
    private final WebServiceTemplate webServiceTemplate;

    @Value("${ibm.mq.queue}")
    private String queueName;

    @Value("${document.api.base-url}")
    private String apiBaseUrl;

    @Value("${document.api.pdf-endpoint}")
    private String pdfEndpoint;

    @Value("${document.poll.timeout-minutes}")
    private int timeoutMinutes;

    @Value("${document.poll.interval-seconds}")
    private int intervalSeconds;

    @Value("${webservice.base-url}")
    private String webserviceBaseUrl;

    @Value("${webservice.resource}")
    private String webserviceResource;

    /**
     * Sends an XML message to the configured MQ queue
     * @param xmlContent the XML content to send
     */
    public void sendToQueue(String xmlContent) {
        log.info("Sending message to queue: {}", queueName);
        try {
            jmsTemplate.convertAndSend(queueName, xmlContent);
            log.info("Message sent successfully");
        } catch (Exception e) {
            log.error("Failed to send message to queue", e);
            throw new RuntimeException("Failed to send message to queue", e);
        }
    }

    /**
     * Sends XML payload to a webservice endpoint
     * @param xmlContent the XML content to send
     * @return the response message from the webservice
     */
    public String sendToWebService(String xmlContent) {
        log.info("Sending message to API: {}/{}", webserviceBaseUrl, webserviceResource);
        var fullUrl = webserviceBaseUrl + "/" + webserviceResource;

        try {
            StreamSource source = new StreamSource(new StringReader(xmlContent));
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            
            webServiceTemplate.sendSourceAndReceiveToResult(fullUrl, source, result);
            
            String response = writer.toString();
            log.info("Message sent successfully");
            return response;
        } catch (Exception e) {
            log.error("Failed to send message to API", e);
            throw new RuntimeException("Failed to send message to API", e);
        }
    }

    /**
     * Retrieves the generated PDF for a given policy number
     * @param policyNumber the policy number to retrieve the PDF for
     * @return the PDF content as a byte array
     */
    public byte[] retrievePDF(String policyNumber) {
        String url = apiBaseUrl + pdfEndpoint;
        log.info("Attempting to retrieve PDF for policy: {} from: {}", policyNumber, url);

        AtomicReference<byte[]> result = new AtomicReference<>();

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(intervalSeconds))
                .untilAsserted(() -> {
                    ResponseEntity<byte[]> response = restTemplate.getForEntity(
                            url + "?policyNumber=" + policyNumber,
                            byte[].class
                    );
                    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                        throw new AssertionError("PDF not yet available");
                    }
                    result.set(response.getBody());
                });

        log.info("Successfully retrieved PDF for policy: {}", policyNumber);
        return result.get();
    }
}