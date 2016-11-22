package com.dotmarketing.util;

import java.util.UUID;

import com.dotcms.repackage.org.apache.logging.log4j.core.util.UuidUtil;

public final class UUIDUtil {

  public static boolean isUUID(final String uuid) {
    return uuid!=null && unUidIfy(uuid).matches("[a-fA-F0-9]{32}");
  }


  public static String uuidIfy(String shorty) {
    StringBuilder newShorty = new StringBuilder();
    shorty = unUidIfy(shorty);
    char[] chars = shorty.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (i == 8 || i == 12 || i == 16 || i == 20) {
        newShorty.append('-');
      }
      newShorty.append(c);
    }
    return newShorty.toString();
  }

  public static String unUidIfy(String shorty) {
    while (shorty.contains("-")) {
      shorty = shorty.replace("-", "");
    }
    return shorty;
  }

  public static String uuid() {
    return UUID.randomUUID().toString();
  }

  public static String uuidTimeBased() {
    return UuidUtil.getTimeBasedUuid().toString();
  }



}
