package com.example.AI_Projekt.repository;

import com.example.AI_Projekt.entity.CollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionItemRepo extends JpaRepository<CollectionItem, Integer> {

    List<CollectionItem> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);

}