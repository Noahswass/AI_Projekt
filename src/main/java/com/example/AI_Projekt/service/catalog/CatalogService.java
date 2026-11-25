package com.example.AI_Projekt.service.catalog;

import com.example.AI_Projekt.entity.LegoSet;
import com.example.AI_Projekt.entity.Part;
import com.example.AI_Projekt.entity.Theme;
import com.example.AI_Projekt.repository.LegoSetRepo;
import com.example.AI_Projekt.repository.PartRepo;
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
import java.util.Optional;
import java.util.Set;

/** Reads the public catalog from the local database. Never calls Rebrickable. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    public static final int PAGE_SIZE = 24;
    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("releaseYear"),
                    Sort.Order.asc("legoSetNum"));
    private static final Sort BY_PART_NAME =
            Sort.by(Sort.Order.asc("name"),
                    Sort.Order.asc("partNum"));
    private static final List<Integer> NO_THEME_FILTER = List.of(-1);

    private final LegoSetRepo legoSetRepo;
    private final ThemeRepo themeRepo;
    private final PartRepo partRepo;

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

    public Page<PartRow> searchParts(String query, int page) {
        String pattern = likePattern(query);
        int index = Math.max(page, 1) - 1;
        Page<Part> result = partRepo.search(pattern,
                PageRequest.of(index, PAGE_SIZE, BY_PART_NAME));
        if (result.isEmpty() && result.getTotalPages() > 0) {
            result = partRepo.search(pattern,
                    PageRequest.of(result.getTotalPages() - 1, PAGE_SIZE, BY_PART_NAME));
        }
        return result.map(part -> new PartRow(
                part.getPartNum(),
                part.getName(),
                part.getPartImgUrl(),
                part.getPartUrl()));
    }

    public Optional<SetDetail> findSet(String setNum) {
        Map<Integer, Theme> themes = themesById();
        return legoSetRepo.findByLegoSetNum(setNum)
                .map(set -> new SetDetail(
                        set.getLegoSetNum(),
                        set.getName(),
                        set.getReleaseYear(),
                        themePath(set.getThemeId(), themes),
                        set.getNumParts(),
                        set.getLegoSetImgUrl(),
                        set.getLegoSetUrl()
                ));
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

    /**
     * The top-level themes that have at least one set, sorted by name. Sets in sub-themes count
     * for their top-level theme, and the picture is the one of the biggest set in the whole group.
     */
    public List<ThemeCard> themeCards() {
        Map<Integer, Theme> themes = themesById();

        Map<Integer, Long> setCounts = new HashMap<>();
        for (LegoSetRepo.ThemeSetCount row : legoSetRepo.countSetsPerTheme()) {
            Integer top = topLevelId(row.getThemeId(), themes);
            if (top != null) {
                setCounts.merge(top, row.getSetCount(), Long::sum);
            }
        }

        Map<Integer, LegoSetRepo.ThemePicture> pictures = new HashMap<>();
        for (LegoSetRepo.ThemePicture row : legoSetRepo.findBiggestSetPicturePerTheme()) {
            Integer top = topLevelId(row.getThemeId(), themes);
            if (top != null) {
                pictures.merge(top, row, (current, candidate) ->
                        parts(candidate) > parts(current) ? candidate : current);
            }
        }

        List<ThemeCard> cards = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : setCounts.entrySet()) {
            Integer id = entry.getKey();
            LegoSetRepo.ThemePicture picture = pictures.get(id);
            cards.add(new ThemeCard(id, themes.get(id).getName(), entry.getValue(),
                    picture == null ? null : picture.getImgUrl()));
        }
        cards.sort(Comparator.comparing(ThemeCard::name, String.CASE_INSENSITIVE_ORDER));
        return cards;
    }

    private int parts(LegoSetRepo.ThemePicture picture) {
        return picture.getNumParts() == null ? -1 : picture.getNumParts();
    }

    /** The id of the theme at the top of this theme's parent chain, or null when the theme is unknown. */
    private Integer topLevelId(Integer themeId, Map<Integer, Theme> themes) {
        Theme theme = themes.get(themeId);
        if (theme == null) {
            return null;
        }
        Set<Integer> seen = new HashSet<>();
        seen.add(theme.getId());
        // Same caution as in themePath: a parent may be missing or (in theory) loop
        while (theme.getParentId() != null && seen.add(theme.getParentId())) {
            Theme parent = themes.get(theme.getParentId());
            if (parent == null) {
                break;
            }
            theme = parent;
        }
        return theme.getId();
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
