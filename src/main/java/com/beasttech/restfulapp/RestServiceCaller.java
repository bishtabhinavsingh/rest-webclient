package com.beasttech.restfulapp;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RestServiceCaller {
    private final WebClient webClient = WebClient.create();

    public ResponseEntity<String> requestAsync(Object message, HttpMethod method, String URL){
        WebClient.RequestBodySpec request = prepareRequest(URL, method, message);
        return request.retrieve()
                .toEntity(String.class)
                .block();
    }

    private WebClient.RequestBodySpec prepareRequest(String URL, HttpMethod method, Object message){
        WebClient.RequestBodySpec request = null;
        if (HttpMethod.GET.equals(method)){
            request = (WebClient.RequestBodySpec) webClient.get().uri(messageToURI(message, URL));
        } else if (HttpMethod.PUT.equals(method)){
            request = (WebClient.RequestBodySpec) webClient.put().uri(URL).body(Mono.just(message), message.getClass());
        } else if (HttpMethod.POST.equals(method)){
            request = (WebClient.RequestBodySpec) webClient.post().uri(URL).body(Mono.just(message), message.getClass());
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