package com.voting.util;

import com.voting.model.Vote;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Implements a Blockchain-inspired immutable ledger for votes.
 * Each vote is linked to the hash of the previous vote, ensuring that 
 * any modification to a single vote invalidates the entire chain.
 */
public class VoteChainUtil {

    /**
     * Calculates the hash of a vote combined with the previous hash.
     * This creates the "chain" effect.
     */
    public static String calculateBlockHash(Vote vote, String previousHash) {
        try {
            String dataToHash = previousHash + 
                               vote.getVoterId() + 
                               vote.getCandidateId() + 
                               vote.getElectionId() + 
                               vote.getTimestamp().getTime();
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedHash);
        } catch (Exception e) {
            throw new RuntimeException("Blockchain hashing failed", e);
        }
    }

    /**
     * Verifies the integrity of a vote by re-calculating its hash 
     * based on the previous known hash in the ledger.
     */
    public static boolean verifyIntegrity(Vote vote, String previousHash, String currentHash) {
        String recalculated = calculateBlockHash(vote, previousHash);
        return recalculated.equals(currentHash);
    }
}
