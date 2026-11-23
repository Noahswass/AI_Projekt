package com.example.AI_Projekt.security;

import com.example.AI_Projekt.entity.AppUser;
import com.example.AI_Projekt.entity.Role;
import com.example.AI_Projekt.repository.AppUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @Test
    public void existingNormalUserWithAdminNameIsLeftAlone() throws Exception {
        // Arrange: "boss" was registered as a normal user before the admin was configured
        String userHash = bCryptPasswordEncoder.encode("users-own-password");
        AppUser existingUser = new AppUser();
        existingUser.setUsername("boss");
        existingUser.setRole(Role.USER);
        existingUser.setPasswordHash(userHash);
        when(appUserRepo.findByUsernameIgnoreCase("boss")).thenReturn(Optional.of(existingUser));

        // Act: a non-blank password, so the blank guard does not stop the run first
        AdminAccountInitializer adminInitializer =
                new AdminAccountInitializer(appUserRepo, bCryptPasswordEncoder, "boss", "secret");
        adminInitializer.run(null);

        // Assert: nothing saved, and the user is not promoted or given the admin password
        verify(appUserRepo, never()).save(any(AppUser.class));
        assertThat(existingUser.getRole()).isEqualTo(Role.USER);
        assertThat(existingUser.getPasswordHash()).isEqualTo(userHash);
    }

}
