package com.example.AI_Projekt.security;

import com.example.AI_Projekt.entity.AppUser;
import com.example.AI_Projekt.repository.AppUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

public class AdminAccountInitializerTest {
    private AppUserRepo appUserRepo;
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @BeforeEach
    public void setUp() {
        appUserRepo = mock(AppUserRepo.class);
        bCryptPasswordEncoder = new BCryptPasswordEncoder();
    }
    @Test
    public void blankPasswordSkipsAdminCreation () throws Exception {
        String username = "boss";
        String password = "";
        AdminAccountInitializer adminInitializer = new AdminAccountInitializer(appUserRepo, bCryptPasswordEncoder, username, password);
        adminInitializer.run(null);
        verify(appUserRepo, never()).save(any(AppUser.class));
    }

}
