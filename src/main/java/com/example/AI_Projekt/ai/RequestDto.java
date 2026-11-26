package com.example.AI_Projekt.ai;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RequestDto(String model, int maxTokens, String system, List<MessageDto> messages) {}
