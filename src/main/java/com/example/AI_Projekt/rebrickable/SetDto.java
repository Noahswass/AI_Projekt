package com.example.AI_Projekt.rebrickable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SetDto(
        @JsonProperty("set_num") String legoSetNum,
        String name,
        Integer year,
        Integer themeId,
        Integer numParts,
        @JsonProperty("set_img_url") String legoSetImgUrl,
        @JsonProperty("set_url") String legoSetUrl,
        Instant lastModifiedDt) {
}
