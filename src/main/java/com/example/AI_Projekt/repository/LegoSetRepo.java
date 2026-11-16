package com.example.AI_Projekt.repository;
import com.example.AI_Projekt.entity.LegoSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface LegoSetRepo extends JpaRepository<LegoSet, Integer> {
    Optional<LegoSet> findByLegoSetNum(String setNum);
}
