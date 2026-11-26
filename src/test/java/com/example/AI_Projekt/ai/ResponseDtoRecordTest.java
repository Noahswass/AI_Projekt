package com.example.AI_Projekt.ai;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponseDtoRecordTest {
    private final ObjectMapper objectMapper;

    public ResponseDtoRecordTest() {
        this.objectMapper = new ObjectMapper();
    }

    @Test
    public void testResponseRecord() throws Exception {
       ResponseDto r = objectMapper.readValue(getClass().getResourceAsStream("/fixtures/llm-message.json"), ResponseDto.class);
       assertThat(r.content().get(0).text()).isEqualTo("Hi there! How can I help you today?");
       assertThat(r.stopReason()).isEqualTo("end_turn");
       assertThat(r.usage().inputTokens()).isEqualTo(12);
       assertThat(r.usage().outputTokens()).isEqualTo(14);
    }
}
