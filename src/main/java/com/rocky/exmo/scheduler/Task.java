package com.rocky.exmo.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rocky.exmo.model.Ask;
import com.rocky.exmo.model.Currency;
import com.rocky.exmo.service.RequestService;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Component
@ConfigurationProperties(prefix = "task")
public class Task {

    private static final Logger log = LoggerFactory.getLogger(Task.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private final RequestService requestService;

    private List<Currency> currencies;
    private String pairs;
    private ObjectMapper mapper;

    public Task(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostConstruct
    public void init() {
        if (CollectionUtils.isEmpty(currencies)) {
            log.error("Currencies are not set.");
            System.exit(0);
        }
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        pairs = currencies.stream()
                .map(Currency::getPair)
                .collect(Collectors.joining(","));
    }

    @Scheduled(fixedRate = 5000)
    public void handle() {
        log.info("The time is now {}", dateFormat.format(new Date()));

        JsonNode response = requestService.getAskPairs(pairs);
        if (response == null) {
            log.error("Can't create request, for pairs : " +  pairs);
            return;
        }
        for (Currency currency : currencies) {
            JsonNode askPair = response.get(currency.getPair());
            if (Objects.isNull(askPair)) {
                log.warn("Can't load ask for pair = " + currency.getPair());
                continue;
            }
            try {
                Ask ask = mapper.treeToValue(askPair, Ask.class);
                // TODO work algorithm
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

    }
}