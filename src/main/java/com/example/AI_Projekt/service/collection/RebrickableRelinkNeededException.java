package com.example.AI_Projekt.service.collection;

public class RebrickableRelinkNeededException extends RuntimeException {
    public RebrickableRelinkNeededException() {
        super("Rebrickable doesnt accept your token, please relink your account");
    }
}