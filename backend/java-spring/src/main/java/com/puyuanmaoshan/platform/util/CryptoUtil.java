package com.puyuanmaoshan.platform.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加密工具类 - 用于加密/解密敏感配置信息
 * 使用 AES-GCM 算法，密钥从环境变量 CRYPTO_SECRET_KEY 读取
 */
@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12; // GCM IV length
    private static final int TAG_LENGTH = 128; // GCM tag length

    private final SecretKeySpec secretKey;

    public CryptoUtil(@Value("${app.crypto.secret-key:puyuan-maoshan-default-secret-key-32bytes}") String secretKeyStr) {
        // 确保密钥长度为 32 字节（256 bits）
        byte[] keyBytes = secretKeyStr.getBytes(StandardCharsets.UTF_8);
        byte[] paddedKey = new byte[32];
        System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
        this.secretKey = new SecretKeySpec(paddedKey, ALGORITHM);
    }

    /**
     * 加密文本
     * @param plainText 明文
     * @return Base64 编码的密文（包含 IV）
     */
    public String encrypt(String plainText) {
        try {
            // 生成随机 IV
            byte[] iv = new byte[IV_LENGTH];
            java.security.SecureRandom random = new java.security.SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 组合 IV 和密文
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密文本
     * @param cipherText Base64 编码的密文（包含 IV），或明文
     * @return 明文
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }

        try {
            // 尝试解密（如果输入是加密数据）
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // 提取 IV 和密文
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 如果解密失败，假设输入是明文，直接返回
            return cipherText;
        }
    }

    /**
     * 脱敏显示 API Key
     * @param apiKey 原始 API Key
     * @return 脱敏后的 API Key（如：sk-****abcd）
     */
    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "";
        }
        int length = apiKey.length();
        if (length <= 8) {
            return "****";
        }
        // 保留前 4 位和后 4 位，中间用 **** 替换
        return apiKey.substring(0, 4) + "****" + apiKey.substring(length - 4);
    }

    /**
     * 脱敏显示密钥
     * @param key 原始密钥
     * @param prefixLen 前缀长度
     * @param suffixLen 后缀长度
     * @return 脱敏后的密钥
     */
    public static String maskKey(String key, int prefixLen, int suffixLen) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        int length = key.length();
        if (length <= prefixLen + suffixLen) {
            return "****";
        }
        return key.substring(0, prefixLen) + "****" + key.substring(length - suffixLen);
    }
}
