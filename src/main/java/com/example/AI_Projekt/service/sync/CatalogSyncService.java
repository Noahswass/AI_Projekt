package com.example.AI_Projekt.service.sync;

import com.example.AI_Projekt.entity.LegoColour;
import com.example.AI_Projekt.entity.LegoSet;
import com.example.AI_Projekt.entity.Minifig;
import com.example.AI_Projekt.entity.Part;
import com.example.AI_Projekt.entity.Theme;
import com.example.AI_Projekt.rebrickable.RebrickableClient;
import com.example.AI_Projekt.repository.LegoColourRepo;
import com.example.AI_Projekt.repository.LegoSetRepo;
import com.example.AI_Projekt.repository.MinifigRepo;
import com.example.AI_Projekt.repository.PartRepo;
import com.example.AI_Projekt.repository.ThemeRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Service
@Transactional
public class CatalogSyncService {

    private final RebrickableClient rebBrickableClient;
    private final ThemeRepo themeRepo;
    private final LegoColourRepo legoColourRepo;
    private final LegoSetRepo legoSetRepo;
    private final PartRepo partRepo;
    private final MinifigRepo minifigRepo;

    public CatalogSyncService(RebrickableClient rebrickableClient, ThemeRepo themeRepo, LegoColourRepo legoColourRepo,
                              LegoSetRepo legoSetRepo, PartRepo partRepo, MinifigRepo minifigRepo) {
        this.rebBrickableClient = rebrickableClient;
        this.themeRepo = themeRepo;
        this.legoColourRepo = legoColourRepo;
        this.legoSetRepo = legoSetRepo;
        this.partRepo = partRepo;
        this.minifigRepo = minifigRepo;

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
        // Load the existing sets once instead of one query per set. A query inside the loop makes
        // Hibernate flush and re-check every set loaded so far, which took about 12 minutes.
        Map<String, LegoSet> existing = new HashMap<>();
        for (LegoSet legoSet : legoSetRepo.findAll()) {
            existing.put(legoSet.getLegoSetNum(), legoSet);
        }
        for (var dto : legoSets) {
            LegoSet legoSet = existing.computeIfAbsent(dto.legoSetNum(), key -> new LegoSet());
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

    public int syncParts() {
        var parts = rebBrickableClient.fetchParts();
        Map<String, Part> existing = new HashMap<>();
        for (Part part : partRepo.findAll()) {
            existing.put(part.getPartNum(), part);
        }
        Timestamp now = Timestamp.from(Instant.now());
        for (var dto : parts) {
            // computeIfAbsent also keeps a part_num that Rebrickable sends twice in one run on one row
            Part part = existing.computeIfAbsent(dto.partNum(), key -> new Part());
            part.setPartNum(dto.partNum());
            part.setName(dto.name());
            part.setPartCatId(dto.partCatId());
            part.setPartImgUrl(dto.partImgUrl());
            part.setPartUrl(dto.partUrl());
            part.setLastSyncDt(now);
            partRepo.save(part);
        }
        return parts.size();
    }

    public int syncMinifigs() {
        var minifigs = rebBrickableClient.fetchMinifigs();
        Map<String, Minifig> existing = new HashMap<>();
        for (Minifig minifig : minifigRepo.findAll()) {
            existing.put(minifig.getFigNum(), minifig);
        }
        Timestamp now = Timestamp.from(Instant.now());
        for (var dto : minifigs) {
            Minifig minifig = existing.computeIfAbsent(dto.figNum(), key -> new Minifig());
            minifig.setFigNum(dto.figNum());
            minifig.setName(dto.name());
            minifig.setNumParts(dto.numParts());
            minifig.setImgUrl(dto.imgUrl());
            minifig.setLastSyncDt(now);
            minifigRepo.save(minifig);
        }
        return minifigs.size();
    }

    /** Runs every step in one transaction: if one step fails, nothing from this run is kept. */
    public SyncCounts syncAll() {
        int themes = syncThemes();
        int colours = syncColours();
        int legoSets = syncLegoSets();
        int parts = syncParts();
        int minifigs = syncMinifigs();
        return new SyncCounts(themes, colours, legoSets, parts, minifigs);
    }

}


