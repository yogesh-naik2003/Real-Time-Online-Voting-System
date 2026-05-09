package com.voting.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class DigitalSignatureUtil {
    private static final String SECRET_KEY = "SEC_STATE_ELECTION_COMMISSION_KEY_2026";

    public static String generateSignature(int userId, int candidateId, String receiptId) {
        try {
            String data = userId + ":" + candidateId + ":" + receiptId + ":" + SECRET_KEY;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static boolean verifySignature(int userId, int candidateId, String receiptId, String signature) {
        String expected = generateSignature(userId, candidateId, receiptId);
        return expected.equals(signature);
    }
}
