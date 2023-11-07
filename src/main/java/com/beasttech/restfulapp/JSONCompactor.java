package com.beasttech.restfulapp;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class JSONCompactor {
    private final WebClient webClient;
    private final Path tempDirectory;

    public JSONCompactor(WebClient.Builder webClientBuilder, String tempDirectoryPath) throws IOException {
        this.webClient = webClientBuilder.baseUrl("http:localhost:8090/upload-batch").build();
        this.tempDirectory = createTempDirectory(tempDirectoryPath);
    }

    private Path createTempDirectory(String tempDirectoryPath) throws IOException {
        Path tempDir = Files.createTempDirectory(tempDirectoryPath);
        return tempDir.toAbsolutePath();
    }

    public void uploadChunks(byte[][] chunks) {
        int totalChunks = chunks.length;

        for (int i = 0; i < totalChunks; i++) {
            byte[] chunkData = chunks[i];
            int chunkNumber = i + 1;
            int currentChunkSize = chunkData.length;

            // Log the current chunk information
            logChunkInfo(chunkNumber, currentChunkSize, totalChunks);

            // Save the current chunk to the temporary directory
            try {
                saveChunkToFile(chunkData, chunkNumber);
            } catch (IOException e) {
                System.err.println("Error saving Chunk " + chunkNumber + " to temporary directory: " + e.getMessage());
                continue; // Skip this chunk and continue with the next one
            }

            // Upload the current chunk
            uploadChunk(chunkNumber)
                    .doOnError(error -> handleChunkUploadError(chunkNumber, error))
                    .subscribe(response -> handleChunkUploadSuccess(chunkNumber, response));
        }
    }

    private void logChunkInfo(int chunkNumber, int chunkSize, int totalChunks) {
        System.out.println("Uploading Chunk " + chunkNumber + " of " + totalChunks +
                " (Size: " + chunkSize + " bytes)");
    }

    private void saveChunkToFile(byte[] chunkData, int chunkNumber) throws IOException {
        String fileName = "chunk_" + chunkNumber + ".dat";
        Path filePath = tempDirectory.resolve(fileName);
        Files.write(filePath, chunkData, StandardOpenOption.CREATE);
    }

    private Mono<ResponseEntity<Void>> uploadChunk(int chunkNumber) {
        String fileName = "chunk_" + chunkNumber + ".dat";
        Path filePath = tempDirectory.resolve(fileName);

        return webClient.post()
                .body(BodyInserters.fromResource(new ByteArrayResource(Files.readAllBytes(filePath))))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.just(ResponseEntity.ok().build());
                    } else {
                        return Mono.just(ResponseEntity.status(response.statusCode()).build());
                    }
                });
    }

    private void handleChunkUploadError(int chunkNumber, Throwable error) {
        System.err.println("Error uploading Chunk " + chunkNumber + ": " + error.getMessage());
        // Add your error handling logic here, e.g., retry, log, or handle differently.
    }

    private void handleChunkUploadSuccess(int chunkNumber, ResponseEntity<Void> response) {
        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println("Chunk " + chunkNumber + " uploaded successfully");
            // Add any success handling logic here if needed.
        } else {
            System.err.println("Chunk " + chunkNumber + " upload failed with status code: " +
                    response.getStatusCodeValue());
            // Add your error handling logic here for failed uploads.
        }
    }
    public static void saveCompactedJsonToFile(String compactedJson, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(compactedJson);
        } catch (IOException e) {
            e.printStackTrace(); // Handle the exception appropriately
        }
    }
}