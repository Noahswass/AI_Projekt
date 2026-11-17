package com.example.AI_Projekt.service;

/**
 * One top-level theme on the landing page. The count and the picture include its sub-themes.
 * {@code imgUrl} is null when no set in the theme has a picture.
 */
public record ThemeCard(Integer id, String name, long setCount, String imgUrl) {
}
