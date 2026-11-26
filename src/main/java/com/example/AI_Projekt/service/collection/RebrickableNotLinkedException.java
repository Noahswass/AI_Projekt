package com.example.AI_Projekt.service.collection;

public class RebrickableNotLinkedException extends RuntimeException {
    public RebrickableNotLinkedException() {
        super("You are not linked to Rebrickable");
    }
}