package com.rocky.exmo.model;

import lombok.Data;

@Data
public class Currency {

    private Double qty;
    private Double maxOrderCount;
    private Double delta;
    private Double percent;
    private String filePattern;
    private String pair;
}
