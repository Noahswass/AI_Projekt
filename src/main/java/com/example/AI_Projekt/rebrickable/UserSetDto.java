package com.example.AI_Projekt.rebrickable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSetDto(Integer quantity, SetDto set) {
}