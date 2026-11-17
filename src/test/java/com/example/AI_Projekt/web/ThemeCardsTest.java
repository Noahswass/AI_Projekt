package com.example.AI_Projekt.web;

import com.example.AI_Projekt.entity.LegoSet;
import com.example.AI_Projekt.entity.Theme;
import com.example.AI_Projekt.repository.LegoSetRepo;
import com.example.AI_Projekt.repository.ThemeRepo;
import com.example.AI_Projekt.service.CatalogService;
import com.example.AI_Projekt.service.ThemeCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The theme cards on the landing page. Technic has a sub-theme (Cars) whose set is the biggest,
 * City has sets without pictures, and Duplo has no sets at all.
 *
 * The page is requested as a logged-in user, so these tests do not depend on whether "/" is open
 * to visitors. That rule has its own test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:themecards;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class ThemeCardsTest {

    private static final int TECHNIC = 1;
    private static final int CARS = 2;      // sub-theme of Technic
    private static final int CITY = 3;
    private static final int DUPLO = 4;     // no sets

    @Autowired
    MockMvc mockMvc;
    @Autowired
    CatalogService catalogService;
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
        saveTheme(DUPLO, "Duplo", null);
        saveSet("42000-1", "Mobile Crane", TECHNIC, 500, "https://img.example/crane.jpg");
        saveSet("42001-1", "Race Car", CARS, 900, "https://img.example/car.jpg");
        saveSet("42002-1", "Huge Unpictured Truck", CARS, 5000, null);
        saveSet("6000-1", "Fire Station", CITY, 300, null);
    }

    @Test
    void oneCardPerTopLevelThemeThatHasSets() {
        List<ThemeCard> cards = catalogService.themeCards();

        assertThat(cards).extracting(ThemeCard::name).containsExactly("City", "Technic");
    }

    @Test
    void subThemeSetsCountForTheTopLevelTheme() {
        ThemeCard technic = card("Technic");

        assertThat(technic.id()).isEqualTo(TECHNIC);
        assertThat(technic.setCount()).isEqualTo(3);
    }

    @Test
    void pictureIsTheBiggestSetThatHasOne() {
        assertThat(card("Technic").imgUrl()).isEqualTo("https://img.example/car.jpg");
        assertThat(card("City").imgUrl()).isNull();
    }

    @Test
    void pageShowsEachThemeWithALinkToItsSets() throws Exception {
        mockMvc.perform(get("/").with(user("someone")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/catalog/sets?theme=1\"")))
                .andExpect(content().string(containsString("href=\"/catalog/sets?theme=3\"")))
                .andExpect(content().string(containsString("src=\"https://img.example/car.jpg\"")))
                .andExpect(content().string(containsString("3 sets")))
                .andExpect(content().string(containsString("1 set<")))
                .andExpect(content().string(containsString("No picture")))
                .andExpect(content().string(not(containsString("Duplo"))))
                .andExpect(content().string(not(containsString("catalog/sets?theme=2"))));
    }

    @Test
    void pageWorksWhenTheCatalogIsEmpty() throws Exception {
        legoSetRepo.deleteAll();
        themeRepo.deleteAll();

        mockMvc.perform(get("/").with(user("someone")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The catalog is empty.")));
    }

    private ThemeCard card(String name) {
        return catalogService.themeCards().stream()
                .filter(card -> card.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private void saveTheme(int id, String name, Integer parentId) {
        Theme theme = new Theme();
        theme.setId(id);
        theme.setName(name);
        theme.setParentId(parentId);
        themeRepo.save(theme);
    }

    private void saveSet(String setNum, String name, int themeId, int numParts, String imgUrl) {
        LegoSet set = new LegoSet();
        set.setLegoSetNum(setNum);
        set.setName(name);
        set.setReleaseYear(2024);
        set.setThemeId(themeId);
        set.setNumParts(numParts);
        set.setLegoSetImgUrl(imgUrl);
        legoSetRepo.save(set);
    }
}
