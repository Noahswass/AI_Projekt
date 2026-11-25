package com.example.AI_Projekt.service;


import com.example.AI_Projekt.rebrickable.ColorDto;
import com.example.AI_Projekt.rebrickable.SetDto;
import com.example.AI_Projekt.rebrickable.ThemeDto;
import com.example.AI_Projekt.repository.LegoColourRepo;
import com.example.AI_Projekt.repository.LegoSetRepo;
import com.example.AI_Projekt.repository.ThemeRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.example.AI_Projekt.rebrickable.RebrickableClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class CatalogSyncServiceTest {
    @MockBean
    private RebrickableClient rebrickableClient;
    @Autowired
    CatalogSyncService catalogSyncService;
    @Autowired
    ThemeRepo themeRepo;
    @Autowired
    LegoSetRepo legoSetRepo;
    @Autowired
    LegoColourRepo legoColourRepo;
    @Test
    void twiceSyncSameRows() {
        when(rebrickableClient.fetchThemes()).thenReturn(List.of(
                new ThemeDto(1, null, "Technic"),
                new ThemeDto(2, 1, "Cars")
        ));
        when(rebrickableClient.fetchSets()).thenReturn(List.of(
                new SetDto("001-1", "Gears", 1965, 2, 43,
                        "https://cdn.rebrickable.com/media/sets/001-1/11530.jpg",
                        "https://rebrickable.com/sets/001-1/gears/",
                        Instant.parse("2024-01-08T19:22:27.085452Z"))

        ));
        when(rebrickableClient.fetchColours()).thenReturn(List.of(
                new ColorDto(0,"Blue","000001",true),
                new ColorDto(1, "Black", "000000",true),
                new ColorDto(2, "White", "FFFFFF",false)
        ));
        catalogSyncService.syncAll();
        assertThat(themeRepo.count()).isEqualTo(2);
        assertThat(legoSetRepo.count()).isEqualTo(1);
        assertThat(legoColourRepo.count()).isEqualTo(3);
        catalogSyncService.syncAll();
        assertThat(themeRepo.count()).isEqualTo(2);
        assertThat(legoSetRepo.count()).isEqualTo(1);
        assertThat(legoColourRepo.count()).isEqualTo(3);

    }
}

