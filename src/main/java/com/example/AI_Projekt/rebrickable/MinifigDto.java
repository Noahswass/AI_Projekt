package com.example.AI_Projekt.rebrickable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** Rebrickable calls a minifig's number {@code set_num} and its picture {@code set_img_url}. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MinifigDto(
        @JsonProperty("set_num") String figNum,
        String name,
        Integer numParts,
        @JsonProperty("set_img_url") String imgUrl) {
}
