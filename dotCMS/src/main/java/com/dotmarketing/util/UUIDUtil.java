package com.dotmarketing.util;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.liferay.util.StringPool;
import org.apache.logging.log4j.core.util.UuidUtil;

import static com.liferay.util.StringPool.FORWARD_SLASH;

public final class UUIDUtil {

  private static final Pattern INODE_PATTERN = Pattern.compile("/[a-f0-9]+-[a-f0-9]+-[a-f0-9]+-[a-f0-9]+-[a-f0-9]+/");

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

  /**
   * Extracts the inode from a string.
   *
   * @param someString the string to search
   * @return the inode if found, otherwise an empty optional
   */
  public static Optional<String> findInode(final String someString) {

    final Matcher matcher = INODE_PATTERN.matcher(someString);

    if (matcher.find()) {
      final String inode = matcher.group().replace(FORWARD_SLASH, StringPool.BLANK);
      return Optional.ofNullable(inode);
    }

    return Optional.empty();
  }

}
