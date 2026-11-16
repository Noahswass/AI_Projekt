package com.example.AI_Projekt.entity;


import jakarta.persistence.Column;
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
    @Column(name = "set_num")
    private String legoSetNum;
    private String name;
    private Integer releaseYear;
    private Integer themeId;
    private Integer numParts;
    @Column (name = "set_img_url")
    private String legoSetImgUrl;
    @Column (name = "set_url")
    private String legoSetUrl;
    private Timestamp lastModifiedDt;
    private Timestamp lastSyncDt;
}
