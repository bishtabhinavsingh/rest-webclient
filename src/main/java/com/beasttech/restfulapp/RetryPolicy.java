//package com.beasttech.restfulapp;
//
//import reactor.core.publisher.Mono;
//
//
//import java.time.Duration;
//import java.util.concurrent.Callable;
//
//public class RetryPolicy {
//    private static final int MAX_RETRIES = 5; // Adjust the maximum number of retries as needed
//
//    public static <T> Mono<T> retryWithBackoff(Callable<Mono<T>> operation) {
//        // Handle exceptions here if needed
//        return Mono.defer(() -> {
//                    try {
//                        return operation.call();
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                })
//                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofMillis(100))
//                        .maxBackoff(Duration.ofSeconds(5)) // Adjust the maximum backoff duration if needed
//                        .doAfterRetry(RetryPolicy::logRetries))
//                .onErrorResume(Mono::error);
//    }
//
//    public static <T> Mono<T> retryWithExponentialBackoff(Callable<Mono<T>> operation) {
//        // Handle exceptions here if needed
//        return Mono.defer(() -> {
//                    try {
//                        return operation.call();
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                })
//                .retryWhen(Retry
//                        .backoff(MAX_RETRIES, Duration.ofMillis(100))
//                        .maxBackoff(Duration.ofSeconds(5)) // Adjust the maximum backoff duration if needed
//                        .jitter(0.5) // Configure exponential backoff
//                        .doAfterRetry(RetryPolicy::logRetries))
//                .onErrorResume(Mono::error);
//    }
//
//    private static void logRetries(Retry.RetrySignal retrySignal){
//        long retryCount = retrySignal.totalRetries();
//        Throwable failure = retrySignal.failure();
//
//        // Calculate the time of the next retry manually
//        Duration initialBackoff = Duration.ofMillis(100); // Initial backoff duration
//        Duration maxBackoff = Duration.ofSeconds(5); // Maximum backoff duration
//        long nextRetryMillis = Math.min(initialBackoff.toMillis() * (1 << retryCount), maxBackoff.toMillis());
//
//        // Print the retry information
//        System.out.println("Retry #" + retryCount + " failed with exception: " + failure.getMessage());
//        System.out.println("Next retry will be made in " + nextRetryMillis + " milliseconds");
//    }
//}
//
//
