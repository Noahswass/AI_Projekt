package com.example.AI_Projekt.web;

import com.example.AI_Projekt.entity.LegoSet;
import com.example.AI_Projekt.entity.Theme;
import com.example.AI_Projekt.repository.LegoSetRepo;
import com.example.AI_Projekt.repository.ThemeRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The public set list, requested without a login. 28 sets: two Technic sets from 2024 and
 * 26 "Fire Station" sets from 2010, so the list has two pages (24 per page, newest first).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:catalog;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class CatalogControllerTest {

    private static final int TECHNIC = 1;
    private static final int CARS = 2;      // sub-theme of Technic
    private static final int CITY = 3;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ThemeRepo themeRepo;
    @Autowired
    LegoSetRepo legoSetRepo;

    @BeforeEach
    void seed() {
        legoSetRepo.deleteAll();
        themeRepo.deleteAll();
        saveTheme(TECHNIC, "Technic", null);
        saveTheme(CARS, "Cars", TECHNIC);
        saveTheme(CITY, "City", null);
        saveSet("42000-1", "Mobile Crane", 2024, TECHNIC);
        saveSet("42001-1", "Race Car", 2024, CARS);
        for (int number = 6000; number <= 6025; number++) {
            saveSet(number + "-1", "Fire Station " + number, 2010, CITY);
        }
    }

    @Test
    void visitorSeesFirstPageWithoutLogin() throws Exception {
        mockMvc.perform(get("/catalog/sets"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mobile Crane")))
                .andExpect(content().string(containsString("Technic &gt; Cars")))
                .andExpect(content().string(containsString("Fire Station 6021")))
                .andExpect(content().string(not(containsString("Fire Station 6022"))))
                .andExpect(content().string(containsString("of <span>28</span> sets")))
                .andExpect(content().string(containsString("/catalog/sets?page=2")));
    }

    @Test
    void secondPageShowsTheRest() throws Exception {
        mockMvc.perform(get("/catalog/sets").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fire Station 6025")))
                .andExpect(content().string(not(containsString("Fire Station 6021"))))
                .andExpect(content().string(not(containsString("Mobile Crane"))));
    }

    @Test
    void searchesByNameIgnoringCase() throws Exception {
        mockMvc.perform(get("/catalog/sets").param("q", "cRaNe"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mobile Crane")))
                .andExpect(content().string(not(containsString("Race Car"))))
                .andExpect(content().string(not(containsString("Fire Station"))));
    }

    @Test
    void searchesBySetNumber() throws Exception {
        mockMvc.perform(get("/catalog/sets").param("q", "42001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Race Car")))
                .andExpect(content().string(not(containsString("Mobile Crane"))));
    }

    @Test
    void filtersByThemeIncludingSubThemes() throws Exception {
        mockMvc.perform(get("/catalog/sets").param("theme", String.valueOf(TECHNIC)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mobile Crane")))
                .andExpect(content().string(containsString("Race Car")))
                .andExpect(content().string(not(containsString("Fire Station"))));
    }

    @Test
    void combinesThemeAndSearchAndKeepsThemInThePageLinks() throws Exception {
        mockMvc.perform(get("/catalog/sets").param("theme", String.valueOf(CITY)).param("q", "fire"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("of <span>26</span> sets")))
                .andExpect(content().string(containsString("/catalog/sets?q=fire&amp;theme=3&amp;page=2")));
    }

    @Test
    void percentSignIsSearchedLiterally() throws Exception {
        mockMvc.perform(get("/catalog/sets").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No sets match your search.")));
    }

    @Test
    void badPageAndThemeValuesFallBackInsteadOfFailing() throws Exception {
        mockMvc.perform(get("/catalog/sets").param("page", "abc").param("theme", "x"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mobile Crane")));
        mockMvc.perform(get("/catalog/sets").param("page", "999"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fire Station 6025")));
    }

    @Test
    void otherPagesStillNeedALogin() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void visitorCannotPostToTheCatalog() throws Exception {
        mockMvc.perform(post("/catalog/sets"))
                .andExpect(status().isForbidden());
    }

    private void saveTheme(int id, String name, Integer parentId) {
        Theme theme = new Theme();
        theme.setId(id);
        theme.setName(name);
        theme.setParentId(parentId);
        themeRepo.save(theme);
    }

    private void saveSet(String setNum, String name, int year, int themeId) {
        LegoSet set = new LegoSet();
        set.setLegoSetNum(setNum);
        set.setName(name);
        set.setReleaseYear(year);
        set.setThemeId(themeId);
        set.setNumParts(100);
        legoSetRepo.save(set);
    }
}
