package com.voting.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SecurityTests {

    @Test
    public void testDigitalSignature() {
        int userId = 1;
        int candidateId = 5;
        String receiptId = "TEST-RECEIPT-123";
        
        String signature = DigitalSignatureUtil.generateSignature(userId, candidateId, receiptId);
        assertNotNull(signature);
        assertTrue(signature.length() > 20);
        
        // Verify same inputs produce same signature
        String signature2 = DigitalSignatureUtil.generateSignature(userId, candidateId, receiptId);
        assertEquals(signature, signature2);
        
        // Verify verification works
        assertTrue(DigitalSignatureUtil.verifySignature(userId, candidateId, receiptId, signature));
        
        // Verify tampering fails
        assertFalse(DigitalSignatureUtil.verifySignature(userId + 1, candidateId, receiptId, signature));
    }

    @Test
    public void testPasswordHashing() {
        String pass = "StrongPass@123";
        String hash = PasswordUtil.hashPassword(pass);
        
        assertTrue(PasswordUtil.verifyPassword(pass, hash));
        assertFalse(PasswordUtil.verifyPassword("WrongPass", hash));
        
        // Test rehash detection
        assertFalse(PasswordUtil.needsRehash(hash));
    }

    @Test
    public void testInputValidation() {
        // Valid email
        assertEquals("test@example.com", InputValidator.email("  test@example.com  "));
        
        // Invalid email
        assertThrows(IllegalArgumentException.class, () -> InputValidator.email("invalid-email"));
        
        // Mobile validation
        assertEquals("9876543210", InputValidator.mobile("9876543210"));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.mobile("12345"));
    }
}
