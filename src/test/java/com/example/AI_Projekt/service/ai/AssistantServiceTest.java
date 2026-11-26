package com.example.AI_Projekt.service.ai;

import com.example.AI_Projekt.ai.LlmClient;
import com.example.AI_Projekt.ai.PromptBuilder;
import com.example.AI_Projekt.service.catalog.CatalogService;
import com.example.AI_Projekt.service.catalog.SetRow;
import com.example.AI_Projekt.service.ai.AssistantService.AssistantAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** PB-17: AssistantService looks up sets from the question, builds the prompt and asks the LLM. */
class AssistantServiceTest {

    private CatalogService catalogService;
    private PromptBuilder promptBuilder;
    private LlmClient llmClient;
    private AssistantService service;

    private final SetRow falcon = new SetRow("75192-1", "Millennium Falcon",
            2017, "Star Wars", 7541, null);
    private final SetRow smallFalcon = new SetRow("7965-1", "Millennium Falcon",
            2011, "Star Wars", 1254, null);
    private final SetRow falconRacer = new SetRow("9999-1", "Falcon Racer",
            2020, "City", 120, null);

    @BeforeEach
    void setUp() {
        catalogService = mock(CatalogService.class);
        promptBuilder = mock(PromptBuilder.class);
        llmClient = mock(LlmClient.class);
        service = new AssistantService(catalogService, promptBuilder, llmClient);

        // any word without its own stub finds nothing
        when(catalogService.searchSets(anyString(), any(), anyInt())).thenReturn(Page.empty());
        when(promptBuilder.buildSystemPromptString(any())).thenReturn("PROMPT");
        when(llmClient.answer(anyString(), anyString())).thenReturn("LLM ANSWER");
    }

    private void found(String word, SetRow... rows) {
        when(catalogService.searchSets(eq(word), isNull(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(rows)));
    }

    @Test
    void blankQuestionGivesFixedAnswerWithoutLookupOrLlmCall() {
        AssistantAnswer forBlank = service.ask("   ");
        AssistantAnswer forNull = service.ask(null);

        assertThat(forBlank.answer()).isNotBlank();
        assertThat(forBlank.sources()).isEmpty();
        assertThat(forNull.answer()).isNotBlank();
        assertThat(forNull.sources()).isEmpty();
        verifyNoInteractions(catalogService, llmClient);
    }

    @Test
    void stopWordsAndShortWordsAreNotSearched() {
        service.ask("Which sets have the Millennium Falcon?");

        verify(catalogService).searchSets(eq("millennium"), isNull(), anyInt());
        verify(catalogService).searchSets(eq("falcon"), isNull(), anyInt());
        verify(catalogService, never()).searchSets(eq("which"), any(), anyInt());
        verify(catalogService, never()).searchSets(eq("sets"), any(), anyInt());
        verify(catalogService, never()).searchSets(eq("the"), any(), anyInt());
    }

    @Test
    void setNumberIsSearchedAsOneWord() {
        service.ask("Tell me about 75192-1");

        verify(catalogService).searchSets(eq("75192-1"), isNull(), anyInt());
    }

    @Test
    void promptFromPromptBuilderAndTheQuestionReachTheLlm() {
        found("falcon", falcon);

        AssistantAnswer result = service.ask("Falcon?");

        verify(promptBuilder).buildSystemPromptString(List.of(falcon));
        verify(llmClient).answer("PROMPT", "Falcon?");
        assertThat(result.answer()).isEqualTo("LLM ANSWER");
    }

    @Test
    void setsFoundByTwoWordsAreSentOnceAndSourcesKeepTheOrder() {
        found("millennium", falcon, smallFalcon);
        found("falcon", falcon, smallFalcon, falconRacer);

        AssistantAnswer result = service.ask("Millennium Falcon");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SetRow>> sent = ArgumentCaptor.forClass(List.class);
        verify(promptBuilder).buildSystemPromptString(sent.capture());
        assertThat(sent.getValue()).containsExactly(falcon, smallFalcon, falconRacer);
        assertThat(result.sources()).containsExactly("75192-1", "7965-1", "9999-1");
    }

    @Test
    void atMostTwentySetsAreSent() {
        List<SetRow> many = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            many.add(new SetRow(i + "-1", "Castle " + i, 2000, "Castle", 100, null));
        }
        when(catalogService.searchSets(eq("castle"), isNull(), anyInt()))
                .thenReturn(new PageImpl<>(many));

        AssistantAnswer result = service.ask("castle");

        assertThat(result.sources()).hasSize(20).startsWith("1-1");
    }
}
