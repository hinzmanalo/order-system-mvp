package com.orderhub.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes for seed data.
 * Run this test to generate new password hashes.
 */
public class PasswordHashGenerator {

    @Test
    public void generatePasswordHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String adminPassword = "admin123";
        String userPassword = "user123";

        String adminHash = encoder.encode(adminPassword);
        String userHash = encoder.encode(userPassword);

        System.out.println("\n=== Password Hashes for Seed Data ===");
        System.out.println("admin@orderhub.com / " + adminPassword);
        System.out.println("Hash: " + adminHash);
        System.out.println("\nuser@orderhub.com / " + userPassword);
        System.out.println("Hash: " + userHash);
        System.out.println("\n=== Verification ===");
        System.out.println("admin123 matches admin hash: " + encoder.matches(adminPassword, adminHash));
        System.out.println("user123 matches user hash: " + encoder.matches(userPassword, userHash));
    }
}
