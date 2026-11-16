package com.example.AI_Projekt.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter

public class LegoColour {
    @Id

    private Integer id;
    private String name;
    private String rgb;
}
