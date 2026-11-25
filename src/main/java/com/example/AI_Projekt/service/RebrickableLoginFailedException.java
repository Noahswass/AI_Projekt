package com.example.AI_Projekt.service;

public class RebrickableLoginFailedException extends RuntimeException {


    public RebrickableLoginFailedException() {
        super("Incorrect username or password");
    }
}


