package com.example.AI_Projekt.service;

/** Someone tried to register a username that already exists. */
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Username is taken: " + username);
    }
}
