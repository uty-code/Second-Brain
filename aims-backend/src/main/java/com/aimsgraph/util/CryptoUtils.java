package com.aimsgraph.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM 기반 암/복호화 유틸리티.
 *
 * <p>암호화 시 매번 고유한 12바이트 IV를 생성하고, [IV (12 bytes) | 암호문 + Auth Tag (16 bytes)] 형태로 결합한 뒤 Base64로
 * 인코딩하여 단일 문자열로 반환합니다.
 *
 * <p>암호화 키는 {@code security.encryption.key} 프로퍼티에서 주입받으며, SHA-256 해싱을 통해 입력 문자열 길이에 무관하게 정확히
 * 32바이트(256비트) AES 키를 파생합니다.
 */
@Component
public class CryptoUtils {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12; // 12 bytes (96 bits) — NIST 권장
  private static final int GCM_TAG_LENGTH = 128; // 128 bits — 최대 인증 강도

  private final SecretKeySpec secretKeySpec;

  public CryptoUtils(@Value("${security.encryption.key}") String encryptionKey) {
    try {
      byte[] keyBytes =
          MessageDigest.getInstance("SHA-256")
              .digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
      this.secretKeySpec = new SecretKeySpec(keyBytes, "AES");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
    }
  }

  /**
   * 평문을 AES-256-GCM으로 암호화합니다.
   *
   * @param plainText 암호화할 평문 (null 또는 빈 문자열이면 그대로 반환)
   * @return Base64 인코딩된 [IV + 암호문 + Auth Tag] 문자열
   */
  public String encrypt(String plainText) {
    if (plainText == null || plainText.isEmpty()) {
      return plainText;
    }
    try {
      // 매 암호화마다 고유한 IV 생성
      byte[] iv = new byte[GCM_IV_LENGTH];
      SecureRandom.getInstanceStrong().nextBytes(iv);

      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec);
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

      // [IV (12 bytes)] + [암호문 + Auth Tag]
      byte[] combined = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      throw new RuntimeException("암호화 처리 중 오류가 발생했습니다", e);
    }
  }

  /**
   * AES-256-GCM으로 암호화된 문자열을 복호화합니다.
   *
   * @param encryptedText Base64 인코딩된 [IV + 암호문 + Auth Tag] 문자열
   * @return 복호화된 평문
   */
  public String decrypt(String encryptedText) {
    if (encryptedText == null || encryptedText.isEmpty()) {
      return encryptedText;
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(encryptedText);

      // 앞 12바이트: IV
      byte[] iv = new byte[GCM_IV_LENGTH];
      System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

      // 나머지: 암호문 + Auth Tag
      byte[] cipherText = new byte[decoded.length - GCM_IV_LENGTH];
      System.arraycopy(decoded, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);
      byte[] decrypted = cipher.doFinal(cipherText);

      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("복호화 처리 중 오류가 발생했습니다", e);
    }
  }
}
