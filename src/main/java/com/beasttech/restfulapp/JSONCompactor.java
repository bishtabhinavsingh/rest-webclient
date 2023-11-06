package com.beasttech.restfulapp;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class JSONCompactor {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final List<Object> batch;
    private final int maxBatchSizeBytes;
    private int currentBatchSizeBytes;

    public JSONCompactor(ObjectMapper objectMapper, WebClient.Builder webClientBuilder) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.baseUrl("http://remote-server.com").build();
        this.batch = new ArrayList<>();
        this.maxBatchSizeBytes = 1024 * 1024 * 2; // 2 MB limit for example
        this.currentBatchSizeBytes = 0;
    }

    public synchronized void addToBatch(Object object) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(object);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        int jsonSize = jsonBytes.length;

        // Check if adding this object exceeds the batch size limit
        if (currentBatchSizeBytes + jsonSize > maxBatchSizeBytes) {
            // If so, send the current batch
            sendBatch();
            // And clear the batch
            batch.clear();
            currentBatchSizeBytes = 0;
        }

        // Add the object to the batch and update the current size
        batch.add(object);
        currentBatchSizeBytes += jsonSize;
    }

    public synchronized void sendBatch() {
        if (!batch.isEmpty()) {
            String compactedJson;
            try {
                compactedJson = objectMapper.writeValueAsString(batch);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize batch", e);
            }

            MultiValueMap<String, Object> bodyValues = new LinkedMultiValueMap<>();
            bodyValues.add("file", new ByteArrayResource(compactedJson.getBytes()) {
                @Override
                public String getFilename() {
                    return "batch.json";
                }
            });

            String response = webClient.post()
                    .uri("/upload-endpoint") // Replace with the actual endpoint
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyValues))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Handle this properly in reactive style

            System.out.println("Batch sent. Server response: " + response);
        }
    }

    // Call this method to flush the remaining batch if needed
    public synchronized void flushBatch() {
        sendBatch();
        batch.clear();
        currentBatchSizeBytes = 0;
    }
}