package com.github.sun.foundation.sql;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class IdGenerator {
  private static SecureRandom sr;
  private static MessageDigest md;

  static {
    try {
      sr = SecureRandom.getInstance("SHA1PRNG");
      md = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      // do nothing
    }
  }

  public static String next() {
    String randomNum = Integer.valueOf(sr.nextInt()).toString();
    byte[] bytes = md.digest(randomNum.getBytes());
    return hexEncode(bytes);
  }

  private static String hexEncode(byte[] input) {
    StringBuilder result = new StringBuilder();
    char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    for (byte b : input) {
      result.append(digits[(b & 0xf0) >> 4]);
      result.append(digits[b & 0x0f]);
    }
    return result.toString();
  }
}
