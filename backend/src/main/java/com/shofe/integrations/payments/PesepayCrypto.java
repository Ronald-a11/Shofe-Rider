package com.shofe.integrations.payments;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-256-CBC as specified in Pesepay's encryption guide:
 * key = 32-character encryption key, IV = first 16 characters of that key.
 * https://developers.pesepay.com/security/encryption/
 */
public final class PesepayCrypto {
    private PesepayCrypto() {}

    public static String encrypt(String json, String encryptionKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key(encryptionKey), iv(encryptionKey));
            return Base64.getEncoder().encodeToString(cipher.doFinal(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not encrypt Pesepay payload");
        }
    }

    public static String decrypt(String payload, String encryptionKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key(encryptionKey), iv(encryptionKey));
            return new String(cipher.doFinal(Base64.getDecoder().decode(payload)), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not decrypt Pesepay payload");
        }
    }

    private static SecretKeySpec key(String encryptionKey) {
        return new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
    }

    private static IvParameterSpec iv(String encryptionKey) {
        return new IvParameterSpec(encryptionKey.substring(0, 16).getBytes(StandardCharsets.UTF_8));
    }
}
