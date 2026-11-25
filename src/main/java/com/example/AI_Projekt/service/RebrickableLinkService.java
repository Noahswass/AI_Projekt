package com.example.AI_Projekt.service;

import com.example.AI_Projekt.entity.AppUser;
import com.example.AI_Projekt.entity.RebrickableAccountLink;
import com.example.AI_Projekt.rebrickable.RebrickableClient;
import com.example.AI_Projekt.rebrickable.RebrickableException;
import com.example.AI_Projekt.repository.AppUserRepo;
import com.example.AI_Projekt.repository.RebrickableAccountLinkRepo;
import com.example.AI_Projekt.security.TokenCipher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;


@Service
@Transactional
public class RebrickableLinkService {
    private final RebrickableClient client;
    private final AppUserRepo appUserRepo;
    private final RebrickableAccountLinkRepo rebrickableAccountLinkRepo;
    private final TokenCipher tokenCipher;


    public RebrickableLinkService(RebrickableClient client, AppUserRepo appUserRepo, RebrickableAccountLinkRepo rebrickableAccountLinkRepo, TokenCipher tokenCipher) {
        this.client = client;
        this.appUserRepo = appUserRepo;
        this.rebrickableAccountLinkRepo = rebrickableAccountLinkRepo;
        this.tokenCipher = tokenCipher;
    }
    private Integer userId(String appUsername) {
        return appUserRepo.findByUsernameIgnoreCase(appUsername)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    public void linkAccount(String appUsername, String rebrickableUsername, String password) {
        AppUser user = appUserRepo.findByUsernameIgnoreCase(appUsername).orElseThrow(() -> new RuntimeException("User not found"));
        String token;
        try {
            token = client.fetchUserToken(rebrickableUsername.trim(), password);
        } catch (RebrickableException e) {
            if (Integer.valueOf(403).equals(e.getStatus())) {
                throw new RebrickableLoginFailedException();
            }
            throw e;
        }
        RebrickableAccountLink link = rebrickableAccountLinkRepo.findByUserId(user.getId()).orElseGet(() -> new RebrickableAccountLink());
        link.setUserId(user.getId());
        link.setEncryptedToken(tokenCipher.encrypt(token));
        link.setLinkedAt(Timestamp.from(Instant.now()));
        rebrickableAccountLinkRepo.save(link);
    }

    public void unlinkAccount(String appUsername) {
        rebrickableAccountLinkRepo.findByUserId(userId(appUsername)).ifPresent(rebrickableAccountLinkRepo::delete);
    }

    @Transactional(readOnly = true)
    public boolean isLinked(String appUsername) {
        return rebrickableAccountLinkRepo.findByUserId(userId(appUsername)).isPresent();
    }

}
