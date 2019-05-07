package com.rocky.exmo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Ask {

        @JsonProperty(value = "ask_quantity")
        private String askQuantity;
        @JsonProperty(value = "ask_amount")
        private String askAmount;
        private String[][] ask;
        // TODO other fields

}
