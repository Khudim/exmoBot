package com.rocky.exmo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(@Value("request.key") String key, @Value("request.secret") String secret) {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(
                Collections.singletonList(
                        (request, body, execution) -> {
                            HttpHeaders headers = request.getHeaders();
                            headers.add("Key", key);
                            headers.add("Sign", createSign(secret));
                            return execution.execute(request, body);
                        }
                )
        );
        return restTemplate;
    }

    //TODO
    public String createSign(String secret){
        return "secret";
    }
}
