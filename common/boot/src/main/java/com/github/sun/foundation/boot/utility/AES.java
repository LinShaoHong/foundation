package com.github.sun.foundation.boot.utility;

import com.google.common.io.BaseEncoding;
import lombok.experimental.UtilityClass;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@UtilityClass
public class AES {
  public String encrypt(String value, String key) {
    try {
      Cipher eCipher = Cipher.getInstance("AES");
      SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");
      eCipher.init(Cipher.ENCRYPT_MODE, secretKey);
      byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
      byte[] enc = eCipher.doFinal(utf8);
      return BaseEncoding.base64().encode(enc);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException ex) {
      throw new RuntimeException(ex);
    }
  }

  public String decrypt(String encrypted, String key) {
    try {
      Cipher dCipher = Cipher.getInstance("AES");
      SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");
      dCipher.init(Cipher.DECRYPT_MODE, secretKey);
      byte[] dec = BaseEncoding.base64().decode(encrypted);
      byte[] utf8 = dCipher.doFinal(dec);
      return new String(utf8, StandardCharsets.UTF_8);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException ex) {
      throw new RuntimeException(ex);
    }
  }
}
