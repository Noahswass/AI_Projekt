package com.example.AI_Projekt.ai;

import com.example.AI_Projekt.service.catalog.SetRow;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class PromptBuilder {
    public String buildSystemPromptString(List<SetRow> sets) {
        StringBuilder sb = new StringBuilder();
                sb.append("You are a LEGO catalog assistant.\n")
                        .append("Answer ONLY using the sets listed under DATA.\n")
                        .append("If you don't know the answer, say 'I don't know'.\n")
                        .append("End your answer with the line 'Sources:' with set numbers used 'Eg:75192-1'.\n\n");
        sb.append("DATA:(set_num | name | release_year | theme_name | num_parts):\n");
        if (sets == null || sets.isEmpty()) {
            sb.append("No matching data");
            sb.append("\n");
        } else {
            sets.stream()
                    .map(s -> String.join(" | ",
                            orUnknown(s.setNum()),
                            orUnknown(s.name()),
                            orUnknown(s.releaseYear()),
                            orUnknown(s.themeName()),
                            orUnknown(s.numParts())))
                    .forEach(line -> sb.append(line).append('\n'));
        }
        return sb.toString();
    }
    private static String orUnknown(Object value) {
        return Objects.toString(value, "Unknown");
    }
}
