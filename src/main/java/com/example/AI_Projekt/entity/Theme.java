package com.example.AI_Projekt.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter

public class Theme {
    @Id
    private int id;
    private String name;
    private Integer parentId;
}

