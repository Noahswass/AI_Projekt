package com.example.AI_Projekt.service.collection;

import com.example.AI_Projekt.entity.AppUser;
import com.example.AI_Projekt.entity.CollectionItem;
import com.example.AI_Projekt.entity.RebrickableAccountLink;
import com.example.AI_Projekt.rebrickable.RebrickableClient;
import com.example.AI_Projekt.rebrickable.RebrickableException;
import com.example.AI_Projekt.rebrickable.UserPartDto;
import com.example.AI_Projekt.rebrickable.UserSetDto;
import com.example.AI_Projekt.repository.AppUserRepo;
import com.example.AI_Projekt.repository.CollectionItemRepo;
import com.example.AI_Projekt.repository.RebrickableAccountLinkRepo;
import com.example.AI_Projekt.security.TokenCipher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CollectionImportService {
    private static final String SET = "SET";
    private static final String PART = "PART";
    private static final int NO_COLOR = -1;
    private final RebrickableClient client;
    private final AppUserRepo appUserRepo;
    private final RebrickableAccountLinkRepo rebrickableAccountLinkRepo;
    private final TokenCipher tokenCipher;
    private final CollectionItemRepo collectionItemRepo;

    public CollectionImportService(RebrickableClient client, AppUserRepo appUserRepo, RebrickableAccountLinkRepo rebrickableAccountLinkRepo, TokenCipher tokenCipher, CollectionItemRepo collectionItemRepo) {
        this.client = client;
        this.appUserRepo = appUserRepo;
        this.rebrickableAccountLinkRepo = rebrickableAccountLinkRepo;
        this.tokenCipher = tokenCipher;
        this.collectionItemRepo = collectionItemRepo;
    }
    public ImportResult importCollection(String appUsername) {
        AppUser user = appUserRepo.findByUsernameIgnoreCase(appUsername).orElseThrow(() -> new RuntimeException("User not found"));
        RebrickableAccountLink link = rebrickableAccountLinkRepo.findByUserId(user.getId()).orElseThrow(() -> new RebrickableNotLinkedException());
        String token = tokenCipher.decrypt(link.getEncryptedToken());
        List<UserSetDto> sets; List<UserPartDto> parts;
        try {
            sets = client.fetchUserSets(token);
            parts = client.fetchUserParts(token);
        } catch (RebrickableException e) {
            if (Integer.valueOf(404).equals(e.getStatus())) {
                throw new RebrickableRelinkNeededException();
            }
            else {
                throw e;
            }
        }
        List<CollectionItem> toSave = new ArrayList<>();
        Map<ItemKey, Integer> wanted = new HashMap<>();
        for(UserSetDto s : sets) {
            wanted.merge(new ItemKey(SET, s.set().legoSetNum(),NO_COLOR),s.quantity(), Integer::sum);
        }
        for(UserPartDto p : parts) {
            wanted.merge(new ItemKey(PART, p.part().partNum(),p.color().id()),p.quantity(), Integer::sum);
        }
        Map<ItemKey, CollectionItem> existing = new HashMap<>();
        for(CollectionItem item : collectionItemRepo.findByUserId(user.getId())) {
            existing.put(new ItemKey(item.getItemType(), item.getItemRef(), item.getColorId()), item);
        }
        for (Map.Entry<ItemKey, Integer> e : wanted.entrySet()) {
            CollectionItem item = existing.remove(e.getKey());
            if (item == null) {
                item = new CollectionItem();
                item.setUserId(user.getId());
                item.setItemType(e.getKey().type());
                item.setItemRef(e.getKey().ref());
                item.setColorId(e.getKey().colorId());
            }
            item.setQuantity(e.getValue());
            toSave.add(item);
        }
        collectionItemRepo.saveAll(toSave);
        collectionItemRepo.deleteAll(existing.values());
        link.setLastImportAt(Timestamp.from(Instant.now()));
        rebrickableAccountLinkRepo.save(link);
        int setCount = (int) wanted.keySet().stream().filter(k -> k.type().equals(SET)).count();
        int partCount = (int) wanted.keySet().stream().filter(k -> k.type().equals(PART)).count();
        return new ImportResult(setCount, partCount);
    }
}
