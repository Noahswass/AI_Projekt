package com.example.AI_Projekt.service.account;

public class RebrickableLoginFailedException extends RuntimeException {


    public RebrickableLoginFailedException() {
        super("Incorrect username or password");
    }
}


