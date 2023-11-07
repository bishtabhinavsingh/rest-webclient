package com.beasttech.restfulapp;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class RetryPolicy {
    private final ExecutorService retryExecutor;
    private final int maxRetries;
    private final long initialDelayMs;
    private final long maxDelayMs;

    public RetryPolicy(int numRetryThreads, int maxRetries, long initialDelayMs, long maxDelayMs) {
        this.retryExecutor = Executors.newFixedThreadPool(numRetryThreads);
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.maxDelayMs = maxDelayMs;
    }

    public void retryChunkUpload(Runnable uploadTask) {
        retryExecutor.submit(() -> {
            int currentRetry = 0;
            long delayMs = initialDelayMs;

            while (currentRetry < maxRetries) {
                try {
                    // Attempt to retry the chunk upload
                    uploadTask.run();

                    // Upload succeeded, break out of the retry loop
                    break;
                } catch (Exception e) {
                    // Handle the error or exception here (e.g., log it)
                    System.err.println("Error during chunk upload retry: " + e.getMessage());

                    // Apply a delay between retry attempts (exponential backoff with a cap)
                    try {
                        Thread.sleep(Math.min(delayMs, maxDelayMs));
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break; // Exit the retry loop if interrupted
                    }

                    // Double the delay for the next retry
                    delayMs *= 2;
                    currentRetry++;
                }
            }
        });
    }

    public void shutdown() {
        retryExecutor.shutdown();
        try {
            if (!retryExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                retryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            retryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

