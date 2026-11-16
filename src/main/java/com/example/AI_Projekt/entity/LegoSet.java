package com.example.AI_Projekt.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;


@Entity
@Getter
@Setter

public class LegoSet {
    @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String setNum;
    private String name;
    private Integer releaseYear;
    private Integer themeId;
    private Integer numParts;
    private String setImgUrl;
    private String setUrl;
    private Timestamp lastModifiedDt;
    private Timestamp lastSyncDt;
}
