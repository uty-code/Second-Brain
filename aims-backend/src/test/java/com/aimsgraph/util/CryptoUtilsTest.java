package com.aimsgraph.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CryptoUtilsTest {

  private CryptoUtils cryptoUtils;
  private final String testKey = "my-secret-test-encryption-key-for-gcm";

  @BeforeEach
  void setUp() {
    // CryptoUtils 생성자를 통해 테스트용 키 수동 주입
    cryptoUtils = new CryptoUtils(testKey);
  }

  @Test
  void encryptAndDecrypt_Success() {
    // Given
    String plainText = "Hello, AIMS-Graph! This is a sensitive API key.";

    // When
    String encryptedText = cryptoUtils.encrypt(plainText);
    String decryptedText = cryptoUtils.decrypt(encryptedText);

    // Then
    assertNotNull(encryptedText);
    assertNotEquals(plainText, encryptedText);
    assertEquals(plainText, decryptedText);
  }

  @Test
  void encrypt_shouldProduceDifferentCiphertextForSamePlaintext() {
    // Given
    String plainText = "Same plain text";

    // When - GCM은 암호화할 때마다 새로운 IV를 사용하므로 매번 다른 암호문이 생성되어야 함
    String encrypted1 = cryptoUtils.encrypt(plainText);
    String encrypted2 = cryptoUtils.encrypt(plainText);

    // Then
    assertNotNull(encrypted1);
    assertNotNull(encrypted2);
    assertNotEquals(encrypted1, encrypted2);

    // 하지만 둘 다 동일한 텍스트로 정상 복호화되어야 함
    assertEquals(plainText, cryptoUtils.decrypt(encrypted1));
    assertEquals(plainText, cryptoUtils.decrypt(encrypted2));
  }

  @Test
  void handleNullOrEmptyString() {
    assertNull(cryptoUtils.encrypt(null));
    assertNull(cryptoUtils.decrypt(null));

    assertEquals("", cryptoUtils.encrypt(""));
    assertEquals("", cryptoUtils.decrypt(""));
  }

  @Test
  void decryptWithInvalidCiphertext_shouldThrowException() {
    assertThrows(RuntimeException.class, () -> cryptoUtils.decrypt("invalid-base64-string"));
    assertThrows(
        RuntimeException.class,
        () -> cryptoUtils.decrypt(Base64.getEncoder().encodeToString("short".getBytes())));
  }
}
