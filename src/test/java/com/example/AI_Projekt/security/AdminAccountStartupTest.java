package com.example.AI_Projekt.security;

import com.example.AI_Projekt.entity.AppUser;
import com.example.AI_Projekt.entity.Role;
import com.example.AI_Projekt.repository.AppUserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_account;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.admin.username=boss",
        "app.admin.password=secret"})
public class AdminAccountStartupTest {
    @Autowired
    private AppUserRepo appUserRepo;

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AdminAccountInitializer adminAccountInitializer;
    @Test
    public void adminIsInitializedAtStartup() {
        AppUser admin = appUserRepo.findByUsernameIgnoreCase("boss").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches("secret", admin.getPasswordHash())).isTrue();
    }
    @Test
    public void adminPasswordIsChangedInDatabase() throws Exception {
        AppUser adminOldPwd = appUserRepo.findByUsernameIgnoreCase("boss").orElseThrow();
        adminOldPwd.setPasswordHash(passwordEncoder.encode("old-password"));
        appUserRepo.save(adminOldPwd);
        adminAccountInitializer.run(null);
        AppUser adminNewPwd = appUserRepo.findByUsernameIgnoreCase("boss").orElseThrow();
        assertThat(passwordEncoder.matches("secret", adminNewPwd.getPasswordHash())).isTrue();
    }
}




