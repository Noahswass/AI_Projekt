package com.example.AI_Projekt.service.ai;

import com.example.AI_Projekt.ai.LlmClient;
import com.example.AI_Projekt.ai.PromptBuilder;
import com.example.AI_Projekt.service.catalog.CatalogService;
import com.example.AI_Projekt.service.catalog.SetRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

// No @Transactional on purpose: the LLM call can take several seconds, and a
// transaction here would hold a DB connection for that whole wait. Each
// searchSets call runs in its own short read-only transaction in CatalogService.

@Service
@RequiredArgsConstructor
public class AssistantService {
    private final CatalogService catalogService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "of", "in", "on", "for", "to", "is", "are",
            "what", "which", "how", "many", "much", "does", "do", "have", "has",
            "set", "sets", "lego", "with", "from", "me", "show", "tell");
    private static final int MAX_SETS = 20;

    public AssistantAnswer ask(String question) {
        if (question == null || question.isBlank()) {
            return new AssistantAnswer("Please ask a question.", List.of());
        }
        // lookup
        List<SetRow> sets = findSets(question);
        // prompt
        String systemPrompt = promptBuilder.buildSystemPromptString(sets);
        String answer = llmClient.answer(systemPrompt, question);
        // answer
        List<String> sources = sets.stream().map(SetRow::setNum).toList();
        return new AssistantAnswer(answer, sources);
    }
    private List<SetRow> findSets(String question) {
        Map<String, SetRow> bySetNum = new LinkedHashMap<>();
        for (String word : extractSearchWords(question)) {
            for (SetRow row : catalogService.searchSets(word, null, 1).getContent()) {
                bySetNum.putIfAbsent(row.setNum(), row);   // no duplicates across words
            }
        }
        return bySetNum.values().stream().limit(MAX_SETS).toList();
    }

    private List<String> extractSearchWords(String question) {
        return Arrays.stream(question.toLowerCase().split("[^\\p{L}\\p{N}-]+"))
                .filter(w -> w.length() > 2)
                .filter(w -> !STOP_WORDS.contains(w))
                .distinct()
                .toList();
    }

    public record AssistantAnswer(String answer, List<String> sources) {

    }

}
