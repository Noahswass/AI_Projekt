package com.example.AI_Projekt.rebrickable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * One page of a paginated Rebrickable list response.
 * {@code next} is null on the last page.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageResponse<T>(int count, String next, String previous, List<T> results) {
}
