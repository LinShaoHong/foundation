package com.github.sun.foundation.boot.utility;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class Hex {
  private final char[] charTable = new char[]{
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    '-', '+'
  };

  public byte[] hex2bytes(String s) {
    int len = s.length();
    if ((len & 1) == 1) {
      throw new IllegalArgumentException("length of string should be even");
    }
    byte[] arr = new byte[len >> 1];
    for (int i = 0, j = 0; i < len; j++) {
      int h = char2hex(s.charAt(i++));
      int l = char2hex(s.charAt(i++));
      arr[j] = (byte) ((h << 4) | l);
    }
    return arr;
  }

  public int char2hex(char c) {
    if (c >= 'a' && c <= 'f') {
      return c - 'a' + 10;
    } else if (c >= 'A' && c <= 'F') {
      return c - 'A' + 10;
    } else if (c >= '0' && c <= '9') {
      return c - '0';
    } else {
      throw new IllegalArgumentException("invalid hex character");
    }
  }

  public String bytes2hex(byte[] buf) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, len = buf.length; i < len; i++) {
      int c = buf[i] & 0xff;
      sb.append(hex2char(c >> 4));
      sb.append(hex2char(c & 0x0f));
    }
    return sb.toString();
  }

  public char hex2char(int c) {
    if (c < 10) {
      return (char) ('0' + c);
    } else if (c < 16) {
      return (char) ('a' + c - 10);
    } else {
      throw new IllegalArgumentException("invalid hex value");
    }
  }

  public String bytes2readable(byte[] buf) {
    StringBuilder sb = new StringBuilder();
    int bits = 0;
    int n = 0;
    for (int i = 0, len = buf.length; i < len; i++) {
      byte b = buf[i];
      n = (n << 8) | (b & 0xff);
      bits += 8;
      do {
        bits -= 6;
        sb.append(charTable[(n >>> bits) & 63]);
      } while (bits >= 6);
    }
    if (bits > 0) {
      sb.append(charTable[(n << (6 - bits)) & 63]);
    }
    return sb.toString();
  }

  public int readable2bytesLen(String value) {
    return value.length() * 6 / 8;
  }

  private final int INDEX_A_LOWER_CASE = indexOf('a');
  private final int INDEX_A_UPPER_CASE = indexOf('A');
  private final int INDEX_MINUS = indexOf('-');

  private int indexOf(char c) {
    for (int i = 0; i < charTable.length; i++) {
      if (charTable[i] == c) {
        return i;
      }
    }
    return -1;
  }

  public int readable2int(char value) {
    if (value >= '0' && value <= '9') {
      return value - '0';
    } else if (value >= 'a' && value <= 'z') {
      return value - 'a' + INDEX_A_LOWER_CASE;
    } else if (value >= 'A' && value <= 'Z') {
      return value - 'A' + INDEX_A_UPPER_CASE;
    } else if (value == '-') {
      return INDEX_MINUS;
    } else if (value == '+') {
      return INDEX_MINUS + 1;
    } else {
      throw new IllegalArgumentException();
    }
  }

  public byte[] readable2bytes(String value) {
    byte[] buf = new byte[readable2bytesLen(value)];
    int bits = 0;
    int n = 0;
    for (int i = 0, j = 0; i < value.length(); i++) {
      int c = readable2int(value.charAt(i));
      n = (n << 6) | c;
      bits += 6;
      while (bits >= 8) {
        bits -= 8;
        buf[j++] = (byte) (n >>> bits);
      }
    }
    return buf;
  }

  public String long2readable(long value) {
    StringBuilder sb = new StringBuilder();
    for (int i = 60; i >= 0; i -= 6) {
      int bits = (int) (value >>> i) & 0x3f;
      sb.append(charTable[bits]);
    }
    return sb.toString();
  }

  public long readable2long(String value) {
    if (value.length() != 11) {
      throw new IllegalArgumentException();
    }
    long n = 0;
    for (int i = 0; i < 11; i++) {
      int c = readable2int(value.charAt(i));
      n = (n << 6) | c;
    }
    return n;
  }
}
