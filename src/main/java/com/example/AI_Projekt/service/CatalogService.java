package com.example.AI_Projekt.service;

import com.example.AI_Projekt.entity.LegoSet;
import com.example.AI_Projekt.entity.Theme;
import com.example.AI_Projekt.repository.LegoSetRepo;
import com.example.AI_Projekt.repository.ThemeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Reads the public catalog from the local database. Never calls Rebrickable. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    public static final int PAGE_SIZE = 24;
    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("releaseYear"), Sort.Order.asc("legoSetNum"));
    private static final List<Integer> NO_THEME_FILTER = List.of(-1);

    private final LegoSetRepo legoSetRepo;
    private final ThemeRepo themeRepo;

    /**
     * @param query   text to find in the name or the set number; null or blank for all sets
     * @param themeId theme to filter on, including its sub-themes; null for all themes
     * @param page    page number starting at 1; a page past the end gives the last page
     */
    public Page<SetRow> searchSets(String query, Integer themeId, int page) {
        Map<Integer, Theme> themes = themesById();
        String pattern = likePattern(query);
        boolean allThemes = themeId == null;
        Collection<Integer> themeIds = allThemes ? NO_THEME_FILTER : withSubThemes(themeId, themes.values());

        int index = Math.max(page, 1) - 1;
        Page<LegoSet> result = legoSetRepo.search(pattern, allThemes, themeIds,
                PageRequest.of(index, PAGE_SIZE, NEWEST_FIRST));
        if (result.isEmpty() && result.getTotalPages() > 0) {
            result = legoSetRepo.search(pattern, allThemes, themeIds,
                    PageRequest.of(result.getTotalPages() - 1, PAGE_SIZE, NEWEST_FIRST));
        }
        return result.map(set -> new SetRow(
                set.getLegoSetNum(),
                set.getName(),
                set.getReleaseYear(),
                themePath(set.getThemeId(), themes),
                set.getNumParts(),
                set.getLegoSetImgUrl()));
    }

    /** All themes for the filter, sorted by their full path. */
    public List<ThemeOption> themeOptions() {
        Map<Integer, Theme> themes = themesById();
        List<ThemeOption> options = new ArrayList<>();
        for (Theme theme : themes.values()) {
            options.add(new ThemeOption(theme.getId(), themePath(theme.getId(), themes)));
        }
        options.sort(Comparator.comparing(ThemeOption::label, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    private Map<Integer, Theme> themesById() {
        Map<Integer, Theme> themes = new HashMap<>();
        for (Theme theme : themeRepo.findAll()) {
            themes.put(theme.getId(), theme);
        }
        return themes;
    }

    /** "%text%" in lower case. The LIKE wildcards % and _ in the user's text are escaped with '!'. */
    private String likePattern(String query) {
        if (query == null || query.isBlank()) {
            return "%";
        }
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    /** The theme itself plus every theme below it. */
    private Set<Integer> withSubThemes(Integer themeId, Collection<Theme> themes) {
        Map<Integer, List<Integer>> children = new HashMap<>();
        for (Theme theme : themes) {
            if (theme.getParentId() != null) {
                children.computeIfAbsent(theme.getParentId(), key -> new ArrayList<>()).add(theme.getId());
            }
        }
        Set<Integer> found = new HashSet<>();
        Deque<Integer> todo = new ArrayDeque<>();
        todo.add(themeId);
        while (!todo.isEmpty()) {
            Integer id = todo.pop();
            if (found.add(id)) {   // false when already seen, which also stops a parent loop
                todo.addAll(children.getOrDefault(id, List.of()));
            }
        }
        return found;
    }

    /** "Parent > Child" for a theme, or null when the theme is unknown. */
    private String themePath(Integer themeId, Map<Integer, Theme> themes) {
        Theme theme = themeId == null ? null : themes.get(themeId);
        if (theme == null) {
            return null;
        }
        String path = theme.getName();
        Set<Integer> seen = new HashSet<>();
        seen.add(theme.getId());
        // theme.parent_id has no foreign key, so a parent may be missing or (in theory) loop
        while (theme.getParentId() != null && seen.add(theme.getParentId())) {
            theme = themes.get(theme.getParentId());
            if (theme == null) {
                break;
            }
            path = theme.getName() + " > " + path;
        }
        return path;
    }
}
