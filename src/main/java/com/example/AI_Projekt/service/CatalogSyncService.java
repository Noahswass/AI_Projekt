package com.example.AI_Projekt.service;
import com.example.AI_Projekt.entity.Theme;
import com.example.AI_Projekt.repository.ThemeRepo;
import com.example.AI_Projekt.rebrickable.RebrickableClient;
import org.springframework.stereotype.Service;



@Service
public class CatalogSyncService {

    private final RebrickableClient rebBrickableClient;
    private final ThemeRepo themeRepo;

    public CatalogSyncService(RebrickableClient rebrickableClient, ThemeRepo themeRepo) {
        this.rebBrickableClient = rebrickableClient;
        this.themeRepo = themeRepo;
    }

    public int syncThemes() {
        var themes = rebBrickableClient.fetchThemes();
        for (var dto : themes) {
            Theme theme = new Theme ();
            theme.setId(dto.id());
            theme.setName(dto.name());
            theme.setParentId(dto.parentId());
            themeRepo.save(theme);
        }
        return themes.size();
    }
}
