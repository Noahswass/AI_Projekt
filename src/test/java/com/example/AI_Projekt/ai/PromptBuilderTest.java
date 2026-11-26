package com.example.AI_Projekt.ai;

import com.example.AI_Projekt.service.catalog.SetRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private static final String FIRST_RULE = "You are a LEGO catalog assistant.";

    private final PromptBuilder promptBuilder = new PromptBuilder();

    private final SetRow falcon = new SetRow("75192-1", "Millennium Falcon",
            2017, "Star Wars", 7541, "https://img.example/75192-1.jpg");

    @Test
    void promptContainsGroundingRules() {
        String prompt = promptBuilder.buildSystemPromptString(List.of(falcon));

        assertThat(prompt)
                .contains("ONLY using the sets listed under DATA")
                .contains("I don't know")
                .contains("Sources:");
    }

    @Test
    void promptContainsOneDataLinePerSet() {
        SetRow explorer = new SetRow("10497-1", "Galaxy Explorer",
                2022, "Icons", 1254, null);

        String prompt = promptBuilder.buildSystemPromptString(List.of(falcon, explorer));

        assertThat(prompt)
                .contains("75192-1 | Millennium Falcon | 2017 | Star Wars | 7541")
                .contains("10497-1 | Galaxy Explorer | 2022 | Icons | 1254");
    }

    @Test
    void nullFieldsShowAsUnknown() {
        SetRow noDetails = new SetRow("1234-1", "Mystery Set", null, "Town", null, null);

        String prompt = promptBuilder.buildSystemPromptString(List.of(noDetails));

        assertThat(prompt)
                .contains("1234-1 | Mystery Set | Unknown | Town | Unknown")
                .doesNotContain("null");
    }

    @Test
    void rulesAppearExactlyOnce() {
        String prompt = promptBuilder.buildSystemPromptString(List.of(falcon));

        assertThat(prompt.indexOf(FIRST_RULE))
                .as("rules should be in the prompt once, not repeated")
                .isEqualTo(prompt.lastIndexOf(FIRST_RULE));
    }

    @Test
    void emptyListSaysNoMatchingDataAndKeepsRules() {
        String prompt = promptBuilder.buildSystemPromptString(List.of());

        assertThat(prompt)
                .contains(FIRST_RULE)
                .contains("I don't know")
                .contains("No matching data");
    }

    @Test
    void nullListIsTreatedLikeEmpty() {
        String prompt = promptBuilder.buildSystemPromptString(null);

        assertThat(prompt).contains("No matching data");
    }

    @Test
    void dataHeaderAppearsOnceWhenEmpty() {
        String prompt = promptBuilder.buildSystemPromptString(List.of());

        assertThat(prompt.indexOf("DATA:"))
                .as("\"DATA:\" should appear once")
                .isEqualTo(prompt.lastIndexOf("DATA:"));
    }
}
