package com.example.AI_Projekt.rebrickable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserTokenDto(
        @JsonProperty("user_token") String userToken){
}