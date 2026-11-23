package com.example.AI_Projekt.service;

import com.example.AI_Projekt.entity.AppUser;
import com.example.AI_Projekt.entity.Role;
import com.example.AI_Projekt.repository.AppUserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Creates accounts. No Rebrickable account is needed; a new account can log in at once.
 */
@Service
public class RegistrationService {

    private final AppUserRepo appUserRepo;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;


    public RegistrationService(AppUserRepo appUserRepo, PasswordEncoder passwordEncoder, @Value("${app.admin.username}") String adminUsername) {
        this.appUserRepo = appUserRepo;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
    }

    /**
     * @throws UsernameTakenException when the username exists already, in any mix of upper and lower case
     */
    public AppUser register(String username, String rawPassword) {
        String name = username.trim();
        if (name.equalsIgnoreCase(adminUsername)) {
            throw new UsernameTakenException(name);
        }
        else if (appUserRepo.existsByUsernameIgnoreCase(name)) {
                throw new UsernameTakenException(name);
        }
        AppUser user = new AppUser();
        user.setUsername(name);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(Role.USER);
        user.setEnabled(true);
        try {
            return appUserRepo.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Two people registered the same name at the same moment: the unique key stopped the second
            throw new UsernameTakenException(name);
        }
    }
}
