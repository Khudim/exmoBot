package com.rocky.exmo.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class RequestService {

    private final RestTemplate restTemplate;
    @Value(value = "${url.ask}")
    private String url;

    public RequestService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JsonNode getAskPairs(String pairs) {
        String uri = UriComponentsBuilder.fromUriString(url)
                .queryParam("limit", 15)
                .queryParam("pair", pairs)
                .toUriString();
        return restTemplate.getForObject(uri, JsonNode.class);
    }
}
