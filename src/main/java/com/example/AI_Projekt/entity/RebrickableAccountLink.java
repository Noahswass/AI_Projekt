package com.example.AI_Projekt.entity;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** An account link to their Rebrickable account in this app.*/
@Entity
@Getter
@Setter
public class RebrickableAccountLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer userId;
    private String encryptedToken;
    private Timestamp linkedAt;
    private Timestamp lastImportAt;
}