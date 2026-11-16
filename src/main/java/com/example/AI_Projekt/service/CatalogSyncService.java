package com.example.AI_Projekt.service;

import com.example.AI_Projekt.entity.LegoColour;
import com.example.AI_Projekt.entity.LegoSet;
import com.example.AI_Projekt.entity.Theme;
import com.example.AI_Projekt.rebrickable.RebrickableClient;
import com.example.AI_Projekt.repository.LegoColourRepo;
import com.example.AI_Projekt.repository.LegoSetRepo;
import com.example.AI_Projekt.repository.ThemeRepo;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;


@Service
public class CatalogSyncService {

    private final RebrickableClient rebBrickableClient;
    private final ThemeRepo themeRepo;
    private final LegoColourRepo legoColourRepo;
    private final LegoSetRepo legoSetRepo;

    public CatalogSyncService(RebrickableClient rebrickableClient, ThemeRepo themeRepo, LegoColourRepo legoColourRepo, LegoSetRepo legoSetRepo) {
        this.rebBrickableClient = rebrickableClient;
        this.themeRepo = themeRepo;
        this.legoColourRepo = legoColourRepo;
        this.legoSetRepo = legoSetRepo;

    }

    public int syncThemes() {
        var themes = rebBrickableClient.fetchThemes();
        for (var dto : themes) {
            Theme theme = new Theme();
            theme.setId(dto.id());
            theme.setName(dto.name());
            theme.setParentId(dto.parentId());
            themeRepo.save(theme);
        }
        return themes.size();
    }

    public int syncColours() {
        var colours = rebBrickableClient.fetchColours();
        for (var dto : colours) {
            LegoColour colour = new LegoColour();
            colour.setId(dto.id());
            colour.setName(dto.name());
            colour.setRgb(dto.rgb());
            legoColourRepo.save(colour);
        }
        return colours.size();
    }

    public int syncLegoSets() {
        var legoSets = rebBrickableClient.fetchSets();
        for (var dto : legoSets) {
            LegoSet legoSet = legoSetRepo.findByLegoSetNum(dto.legoSetNum()).orElseGet(LegoSet::new);
            legoSet.setLegoSetNum(dto.legoSetNum());
            legoSet.setName(dto.name());
            legoSet.setReleaseYear(dto.year());
            legoSet.setLegoSetImgUrl(dto.legoSetImgUrl());
            legoSet.setLegoSetUrl(dto.legoSetUrl());
            legoSet.setThemeId(dto.themeId());
            legoSet.setNumParts(dto.numParts());
            legoSet.setLastModifiedDt(Timestamp.from(dto.lastModifiedDt()));
            legoSet.setLastSyncDt(Timestamp.from(Instant.now()));
            legoSetRepo.save(legoSet);
        }

        return legoSets.size();
    }


}

