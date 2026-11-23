package com.example.AI_Projekt.web;


import com.example.AI_Projekt.service.CatalogService;
import com.example.AI_Projekt.service.PartRow;
import com.example.AI_Projekt.service.SetDetail;
import com.example.AI_Projekt.service.SetRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

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
        model.addAttribute("pageUrl", pageUrl("/catalog/sets", query, themeId, currentPage));
        model.addAttribute("prevUrl", sets.hasPrevious() ? pageUrl("/catalog/sets", query, themeId, currentPage - 1) : null);
        model.addAttribute("nextUrl", sets.hasNext() ? pageUrl("/catalog/sets",query, themeId, currentPage + 1) : null);
        return "catalog/sets";
    }

    @GetMapping("/catalog/sets/{setNum}")
    public String setByNum(@PathVariable String setNum, Model model) {
        Optional<SetDetail> setDetail = catalogService.findSet(setNum);
        if (setDetail.isPresent()) {
            model.addAttribute("setDetail", setDetail.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Set not found");
        }
        return "catalog/set";
    }

    @GetMapping("/catalog/parts")
    public String parts(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String page,
                        Model model) {
        String query = clean(q);
        Integer requestedPage = toInteger(page);
        Page<PartRow> parts = catalogService.searchParts(query, requestedPage == null ? 1 : requestedPage);
        int currentPage = parts.getNumber() + 1;

        model.addAttribute("parts", parts);
        model.addAttribute("q", query);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("firstRow", parts.isEmpty() ? 0 : parts.getNumber() * parts.getSize() + 1);
        model.addAttribute("lastRow", parts.getNumber() * parts.getSize() + parts.getNumberOfElements());
        model.addAttribute("pageUrl", pageUrl("/catalog/parts", query, null, currentPage));
        model.addAttribute("prevUrl", parts.hasPrevious() ? pageUrl("/catalog/parts", query, null, currentPage - 1) : null);
        model.addAttribute("nextUrl", parts.hasNext() ? pageUrl("/catalog/parts", query, null, currentPage + 1) : null);
        return "catalog/parts";
    }

    private String pageUrl(String path, String query, Integer themeId, int page) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath(path);
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
