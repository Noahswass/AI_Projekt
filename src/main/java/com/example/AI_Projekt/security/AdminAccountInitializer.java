package com.example.AI_Projekt.security;

import com.example.AI_Projekt.entity.AppUser;
import com.example.AI_Projekt.entity.Role;
import com.example.AI_Projekt.repository.AppUserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class AdminAccountInitializer implements ApplicationRunner {
    private final AppUserRepo appUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);


    public AdminAccountInitializer
            (AppUserRepo appUserRepo,
             PasswordEncoder passwordEncoder,
             @Value("${app.admin.username}") String adminUsername,
             @Value("${app.admin.password}") String adminPassword) {
        this.appUserRepo = appUserRepo;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (adminPassword.isBlank()) {
            log.warn("ADMIN_PWD is empty, skipping admin account initialization");
            return;
        }
        AppUser admin = appUserRepo.findByUsernameIgnoreCase(adminUsername).orElseGet(AppUser::new);
        if (admin.getRole() == Role.USER) {
            log.error("USERNAME:{} HAS_ROLE_{} As a normal user pick another ADMIN_USER", admin.getUsername(), admin.getRole());
            return;
        }
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        appUserRepo.save(admin);
        log.info("Admin account initialized with username: {}", adminUsername);
    }

}

