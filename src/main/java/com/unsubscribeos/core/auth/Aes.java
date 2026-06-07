package com.unsubscribeos.core.auth;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-GCM authenticated encryption. The 12-byte random IV is prepended to the
 * ciphertext so a single opaque blob round-trips, and GCM's tag protects against
 * tampering of the stored token file.
 */
public final class Aes {

    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RNG = new SecureRandom();

    private Aes() {}

    public static SecretKey newKey() {
        byte[] key = new byte[32];
        RNG.nextBytes(key);
        return new SecretKeySpec(key, "AES");
    }

    public static SecretKey keyFrom(byte[] raw) {
        return new SecretKeySpec(raw, "AES");
    }

    public static byte[] encrypt(SecretKey key, byte[] plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            RNG.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] body = cipher.doFinal(plaintext);
            byte[] out = Arrays.copyOf(iv, iv.length + body.length);
            System.arraycopy(body, 0, out, iv.length, body.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public static byte[] decrypt(SecretKey key, byte[] blob) {
        try {
            byte[] iv = Arrays.copyOfRange(blob, 0, IV_LENGTH);
            byte[] body = Arrays.copyOfRange(blob, IV_LENGTH, blob.length);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(body);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed (corrupt or tampered token store)", e);
        }
    }
}
