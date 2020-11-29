package com.github.sun.foundation.boot.utility;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import com.github.stuxuhai.jpinyin.PinyinException;
import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import lombok.experimental.UtilityClass;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class Pinyins {
  private final Set<String> surnames;

  static {
    String names = IO.read(Pinyins.class.getResourceAsStream("/surname.txt"));
    surnames = Stream.of(names.split("\n")).collect(Collectors.toSet());
  }

  public String spell(String name) {
    StringBuilder sb = new StringBuilder();
    name.codePoints().forEach(c -> {
      if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
        try {
          String pinyin = PinyinHelper.convertToPinyinString(new String(Character.toChars(c)), "", PinyinFormat.WITHOUT_TONE);
          sb.append(pinyin);
        } catch (PinyinException ex) {
          // DO NOTHING
        }
      } else if (Character.isAlphabetic(c) || Character.isDigit(c)) {
        sb.appendCodePoint(c);
      }
    });
    return sb.toString();
  }

  public boolean isChineseSurname(String name) {
    String simpleName = ChineseHelper.convertToSimplifiedChinese(name);
    return simpleName.length() > 1 && simpleName.length() < 6
      && simpleName.codePoints().noneMatch(c -> Character.UnicodeScript.of(c) != Character.UnicodeScript.HAN)
      && surnames.stream().anyMatch(simpleName::startsWith);
  }
}
