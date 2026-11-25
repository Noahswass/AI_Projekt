package com.example.AI_Projekt.service.catalog;

/** One set with everything the catalog detail page shows. */
public record SetDetail(String setNum, String name, Integer releaseYear, String themeName, Integer numParts,
                        String imgUrl, String setUrl) {
}
