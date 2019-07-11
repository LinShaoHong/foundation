package com.github.sun.foundation.boot.utility;

/**
 * @Author LinSH
 * @Date: 12:20 AM 2019-07-11
 */
public class PrependStringBuilder implements CharSequence {
  private char[] buffer;
  private int start;
  private int end;

  public PrependStringBuilder() {
    this.buffer = new char[16];
    this.start = this.end = 0;
  }

  public PrependStringBuilder(int capacity) {
    this.buffer = new char[capacity];
    this.start = this.end = 0;
  }

  public PrependStringBuilder prepend(char c) {
    ensureSize(1, false);
    this.start -= 1;
    this.buffer[this.start] = c;
    return this;
  }

  public PrependStringBuilder prepend(String str) {
    int len = str.length();
    ensureSize(len, false);
    this.start -= len;
    str.getChars(0, len, buffer, this.start);
    return this;
  }

  public PrependStringBuilder append(char c) {
    ensureSize(1, true);
    buffer[this.end] = c;
    this.end += 1;
    return this;
  }

  public PrependStringBuilder append(String str) {
    int len = str.length();
    ensureSize(len, true);
    str.getChars(0, len, buffer, this.end);
    this.end += len;
    return this;
  }

  public void setLength(int length) {
    int curLen = this.end - this.start;
    if (length >= curLen) {
      throw new IndexOutOfBoundsException();
    }
    this.end = this.start + length;
  }

  private void ensureSize(int size, boolean append) {
    int len = buffer.length;
    size += len;
    while (len < size) {
      len = len * 2;
    }
    char[] buf = new char[len];
    if (append) { // 移至开始
      this.end -= this.start;
      System.arraycopy(this.buffer, this.start, buf, 0, this.end);
      this.start = 0;
    } else { // 移至末尾
      int newStart = buf.length - (this.end - this.start);
      System.arraycopy(this.buffer, this.start, buf, newStart, this.end - this.start);
      this.start = newStart;
      this.end = buf.length;
    }
    this.buffer = buf;
  }

  @Override
  public String toString() {
    return new String(buffer, start, end - start).intern();
  }

  @Override
  public int length() {
    return end - start;
  }

  @Override
  public char charAt(int index) {
    return buffer[start + index];
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    int newStart = this.start + start;
    int newEnd = newStart + (end - start);
    if (newStart < this.start || newEnd > this.end) {
      throw new IndexOutOfBoundsException();
    }
    this.start = newStart;
    this.end = newEnd;
    return this;
  }
}
