package com.example.AI_Projekt.repository;

import com.example.AI_Projekt.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepo extends JpaRepository<AppUser, Integer> {

    // Ignoring case in both, so "Anna" and "anna" are one account on every database.
    // MySQL compares usernames without case by default; H2 does not.
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}
