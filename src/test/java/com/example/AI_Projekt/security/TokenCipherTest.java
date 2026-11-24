package com.example.AI_Projekt.security;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TokenCipherTest {
    private TokenCipher tokenCipher;

    @BeforeEach
    public void setUp() {
        tokenCipher = new TokenCipher("TL6PdGYQcF6q3iSmT7d4zffXdMWgeMBJAxDFrVNZsOg=");
    }

    @Test
    public void roundTrip(){
        String token = tokenCipher.decrypt(tokenCipher.encrypt("abc"));
        assertEquals("abc", token);
    }
    @Test
    public void freshIV(){
        String token = tokenCipher.encrypt("abc");
        String token2 = tokenCipher.encrypt("abc");
        assertNotEquals(token, token2);
    }
    @Test
    public void tamper(){
        String token = tokenCipher.encrypt("abc");
        String tamperedToken = token.substring(0, token.length() - 1) + "d";
        assertThrows(TokenCipherException.class, () -> tokenCipher.decrypt(tamperedToken));
    }
    @Test
    public void badKey(){
        assertThrows(TokenCipherException.class, () -> new TokenCipher("gOROGXfeYcco5wiRMmDCDg=="));
    }
}
