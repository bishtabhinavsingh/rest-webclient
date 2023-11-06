package com.beasttech.restfulapp;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RestServiceCaller {

    public Mono<ResponseEntity<String>> request(String message, HttpMethod method, String URL, boolean file){

        WebClient.ResponseSpec request = prepareRequest(URL, method, message, file).retrieve();
        Mono<ResponseEntity<String>> responseMono = responseHandler(request);
        return responseMono
                .subscribeOn(Schedulers.elastic())
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

    private WebClient.RequestBodySpec prepareRequest(String URL, HttpMethod method, String message, boolean file){
        WebClient webClient = WebClient.create();
        WebClient.RequestBodySpec request = null;
        if (HttpMethod.GET.equals(method)){
            request = (WebClient.RequestBodySpec) webClient.get().uri(messageToURI(message, URL));
        } else if (HttpMethod.PUT.equals(method) || HttpMethod.POST.equals(method)){
            if (!file) request = (WebClient.RequestBodySpec) webClient.method(method).uri(URL).body(Mono.just(message), String.class);
            else {
                MultiValueMap<String, Object> formData = prepareMultiPart(message);
                request = (WebClient.RequestBodySpec) webClient.post()
                        .uri(URL)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(formData));
            }
        } else if (HttpMethod.DELETE.equals(method)){
            request = (WebClient.RequestBodySpec) webClient.delete().uri(URL+"/"+message);
    }
        return request;
    }

    private MultiValueMap<String, Object> prepareMultiPart(String message){
        File file = new File(message);
        FileSystemResource fileResource = new FileSystemResource(file);
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", fileResource);
        formData.add("param1", "value1");
        formData.add("param2", "value2");
        return formData;
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