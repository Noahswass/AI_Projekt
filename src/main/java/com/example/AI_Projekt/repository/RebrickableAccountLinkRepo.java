package com.example.AI_Projekt.repository;

import com.example.AI_Projekt.entity.RebrickableAccountLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RebrickableAccountLinkRepo extends JpaRepository<RebrickableAccountLink, Integer> {

    Optional<RebrickableAccountLink> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);

    Integer Id(Integer id);
}
