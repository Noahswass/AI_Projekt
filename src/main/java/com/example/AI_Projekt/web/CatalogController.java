package com.example.AI_Projekt.web;

import com.example.AI_Projekt.service.CatalogService;
import com.example.AI_Projekt.service.SetRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

/** Public catalog pages. Everything is read from the local database. */
@Controller
@RequiredArgsConstructor
public class CatalogController {

    private static final int MAX_QUERY_LENGTH = 100;

    private final CatalogService catalogService;

    // The parameters are read as text and parsed here, so a hand-edited URL such as
    // ?page=abc shows the first page instead of an error.
    @GetMapping("/catalog/sets")
    public String sets(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String theme,
                       @RequestParam(required = false) String page,
                       Model model) {
        String query = clean(q);
        Integer themeId = toInteger(theme);
        Integer requestedPage = toInteger(page);

        Page<SetRow> sets = catalogService.searchSets(query, themeId, requestedPage == null ? 1 : requestedPage);
        int currentPage = sets.getNumber() + 1;

        model.addAttribute("sets", sets);
        model.addAttribute("themes", catalogService.themeOptions());
        model.addAttribute("q", query);
        model.addAttribute("themeId", themeId);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("firstRow", sets.isEmpty() ? 0 : sets.getNumber() * sets.getSize() + 1);
        model.addAttribute("lastRow", sets.getNumber() * sets.getSize() + sets.getNumberOfElements());
        model.addAttribute("prevUrl", sets.hasPrevious() ? pageUrl(query, themeId, currentPage - 1) : null);
        model.addAttribute("nextUrl", sets.hasNext() ? pageUrl(query, themeId, currentPage + 1) : null);
        return "catalog/sets";
    }

    private String pageUrl(String query, Integer themeId, int page) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath("/catalog/sets");
        if (query != null) {
            url.queryParam("q", query);
        }
        if (themeId != null) {
            url.queryParam("theme", themeId);
        }
        return url.queryParam("page", page).build().encode().toUriString();
    }

    private String clean(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() > MAX_QUERY_LENGTH ? trimmed.substring(0, MAX_QUERY_LENGTH) : trimmed;
    }

    private Integer toInteger(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
