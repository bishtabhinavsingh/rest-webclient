package com.beasttech.restfulapp;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RestServiceCaller {
    private final WebClient webClient = WebClient.create();
//    ResponseEntity<String> response;

    public Mono<ResponseEntity<String>> request(String message, HttpMethod method, String URL){
        boolean async = true;

        WebClient.ResponseSpec request = prepareRequest(URL, method, message).retrieve();
        Mono<ResponseEntity<String>> responseMono = responseHandler(request);
        if (async){
            responseMono.subscribe(
                    responseEntity -> {
                        System.out.println("SUCCESS: " + responseEntity.getBody());
                        },
                    error -> {
                        System.out.println("Error: " + error.getMessage());
                    }
            );
            return Mono.just(ResponseEntity.status(HttpStatus.ACCEPTED).body("Async request initiated."))
                    .then(responseMono);
        } else {
            responseMono.onErrorResume(Exception.class,Mono::error).block();
            return responseMono;
        }

    }

    private Mono<ResponseEntity<String>> responseHandler(WebClient.ResponseSpec request) {
        // Handle server/client error here
        return request.onStatus(
                        HttpStatus::is4xxClientError,
                        clientResponse -> {
                            // Retrying policy handled by CustomRetry
                            System.out.println("CLIENT ERROR !!!");
                            return Mono.error(new Exception("Client Error"));
                        })
                    .onStatus(HttpStatus::is5xxServerError,
                        clientResponse -> {
                            // Retry policy handled by CustomRetry
                            System.out.println("SERVER ERROR !!!");
                            return Mono.error(new Exception("SERVER Error"));
                        })
                    .bodyToMono(String.class)
                    .map(
                            responseBody -> ResponseEntity.status(HttpStatus.OK).body(responseBody)
                        );
    }

    private WebClient.RequestBodySpec prepareRequest(String URL, HttpMethod method, String message){
        WebClient.RequestBodySpec request = null;
        if (HttpMethod.GET.equals(method)){
            request = (WebClient.RequestBodySpec) webClient.get().uri(messageToURI(message, URL));
        } else if (HttpMethod.PUT.equals(method) || HttpMethod.POST.equals(method)){
            request = (WebClient.RequestBodySpec) webClient.method(method).uri(URL).bodyValue(message);
        } else if (HttpMethod.DELETE.equals(method)){
            request = (WebClient.RequestBodySpec) webClient.delete().uri(URL+"/"+message);
    }
        return request;
    }

    private String messageToURI(Object message, String URL){
        if (message instanceof String) return URL+"?variable="+message;
        if (message instanceof Map)
            return ((Map<?, ?>) message).entrySet()
                .stream()
                    .map(x -> x.getKey()+"?"+x.getValue())
                    .collect(Collectors.joining("&"));
        return null;
    }

}