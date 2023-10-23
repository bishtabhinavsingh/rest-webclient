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

    public Mono<ResponseEntity<String>> request(String message, HttpMethod method, String URL){


        WebClient.ResponseSpec request = prepareRequest(URL, method, message).retrieve();
        Mono<ResponseEntity<String>> responseMono = responseHandler(request);
        return responseMono
                .doOnSuccess(responseEntity -> System.out.println("SUCCESS: " + responseEntity.getBody()))
                .doOnError(error -> System.out.println("Error: " + error.getMessage()))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).body("Async request initiated."))
                .onErrorResume(Exception.class, error -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request.")));

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
        WebClient webClient = WebClient.create();
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