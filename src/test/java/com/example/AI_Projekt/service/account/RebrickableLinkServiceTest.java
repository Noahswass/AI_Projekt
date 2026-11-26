package com.example.AI_Projekt.service.account;

import com.example.AI_Projekt.entity.AppUser;
import com.example.AI_Projekt.entity.RebrickableAccountLink;
import com.example.AI_Projekt.rebrickable.RebrickableClient;
import com.example.AI_Projekt.rebrickable.RebrickableException;
import com.example.AI_Projekt.repository.AppUserRepo;
import com.example.AI_Projekt.repository.RebrickableAccountLinkRepo;
import com.example.AI_Projekt.security.TokenCipher;
import com.example.AI_Projekt.service.account.RebrickableLoginFailedException;
import com.example.AI_Projekt.service.account.RebrickableLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class RebrickableLinkServiceTest {
    private TokenCipher tokenCipher;
    private AppUserRepo appUserRepo;
    private RebrickableClient client;
    private RebrickableAccountLinkRepo rebrickableAccountLinkRepo;
    private AppUser appUser;
    private RebrickableLinkService rebrickableLinkService;


    @BeforeEach
    public void setUp() {
        appUserRepo = mock(AppUserRepo.class);
        client = mock(RebrickableClient.class);
        rebrickableAccountLinkRepo = mock(RebrickableAccountLinkRepo.class);
        tokenCipher = new TokenCipher("TL6PdGYQcF6q3iSmT7d4zffXdMWgeMBJAxDFrVNZsOg=");
        appUser = new AppUser();
        appUser.setId(7);
        appUser.setUsername("anna");
        when(appUserRepo.findByUsernameIgnoreCase("anna")).thenReturn(Optional.of(appUser));
        rebrickableLinkService = new RebrickableLinkService(client, appUserRepo, rebrickableAccountLinkRepo, tokenCipher);
    }

    @Test
    void linkSavesEncryptedToken() {
        when(rebrickableAccountLinkRepo.findByUserId(7)).thenReturn(Optional.empty());
        when(client.fetchUserToken("rbuser","pw")).thenReturn("tok123");
        rebrickableLinkService.linkAccount("anna", "rbuser", "pw");
        ArgumentCaptor<RebrickableAccountLink> linkCaptor = ArgumentCaptor.forClass(RebrickableAccountLink.class);
        verify(rebrickableAccountLinkRepo).save(linkCaptor.capture());
        RebrickableAccountLink link = linkCaptor.getValue();
        assertThat(link.getEncryptedToken()).isNotEqualTo("tok123");
        assertThat(tokenCipher.decrypt(link.getEncryptedToken())).isEqualTo("tok123");
        assertThat(link.getUserId()).isEqualTo(7);
        assertThat(link.getLinkedAt()).isNotNull();
    }

    @Test
    void reLinkUpdatesSameRow() {
        RebrickableAccountLink existing = new RebrickableAccountLink();
        existing.setUserId(7);
        existing.setEncryptedToken("oldEnc");
        when(rebrickableAccountLinkRepo.findByUserId(7)).thenReturn(Optional.of(existing));
        when(client.fetchUserToken("rbuser","pw")).thenReturn("tok456");
        rebrickableLinkService.linkAccount("anna", "rbuser", "pw");
        ArgumentCaptor<RebrickableAccountLink> linkCaptor = ArgumentCaptor.forClass(RebrickableAccountLink.class);
        verify(rebrickableAccountLinkRepo).save(linkCaptor.capture());
        RebrickableAccountLink link = linkCaptor.getValue();
        assertThat(link).isSameAs(existing);
        assertThat(tokenCipher.decrypt(link.getEncryptedToken())).isEqualTo("tok456");
    }

    @Test
    void wrongPwdThrowsAndNothingIsSaved() {
        when(client.fetchUserToken("rbuser","wrongpwd"))
                .thenThrow(new RebrickableException("any message",403, null));
        assertThatThrownBy(() -> rebrickableLinkService.linkAccount("anna", "rbuser", "wrongpwd"))
                .isInstanceOf(RebrickableLoginFailedException.class);
        verify(rebrickableAccountLinkRepo, never()).save(any());
    }
    @Test
    void timeoutThrowsAndNothingIsSaved() {
        when(client.fetchUserToken("rbuser","pw"))
                .thenThrow(new RebrickableException("timeout",null, null));
        assertThatThrownBy(() -> rebrickableLinkService.linkAccount("anna", "rbuser", "pw"))
                .isInstanceOf(RebrickableException.class);
        verify(rebrickableAccountLinkRepo, never()).save(any());
    }
}