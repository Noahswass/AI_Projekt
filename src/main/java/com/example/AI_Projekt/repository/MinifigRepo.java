package com.example.AI_Projekt.repository;

import com.example.AI_Projekt.entity.Minifig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinifigRepo extends JpaRepository<Minifig, Integer> {
}
