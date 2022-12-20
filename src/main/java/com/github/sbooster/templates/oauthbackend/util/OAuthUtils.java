package com.github.sbooster.templates.oauthbackend.util;

import java.util.Random;

public class OAuthUtils {
    public static String generateRandomPassword() {
        byte[] bytes = new byte[16];
        new Random().nextBytes(bytes);
        return new String(bytes);
    }
}
