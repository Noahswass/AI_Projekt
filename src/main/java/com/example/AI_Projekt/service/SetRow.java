package com.example.AI_Projekt.service;

/** One set as shown in the catalog list. */
public record SetRow(String setNum, String name,
                     Integer releaseYear, String themeName,
                     Integer numParts,
                     String imgUrl) {
}
