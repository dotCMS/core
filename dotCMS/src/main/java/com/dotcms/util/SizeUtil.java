package com.dotcms.util;


import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SizeUtil implements Serializable {


    private static final Pattern SIZE_PATTERN = Pattern.compile("([0-9]+)\\s?(b|kb|mb|gb|k|m|g)?", Pattern.CASE_INSENSITIVE);
    public static long convertToBytes(String size) {
        if (UtilMethods.isEmpty(size)) {
            Logger.warn(SizeUtil.class, "Size is null or empty, returning 0");
            return 0;
        }
        size = size.trim().toLowerCase();


        if(size.matches("[0-9]+")) {
            return Long.parseLong(size);
        }
        Matcher matcher = SIZE_PATTERN.matcher(size);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid size format- got: " + size);
        }
        int sizeInt = Integer.parseInt(matcher.group(1));

        String unit = matcher.groupCount() > 1 ? matcher.group(2): null;
        if(unit == null) {
            return sizeInt;
        }
        if(unit.startsWith("k") ) {
            return sizeInt * 1024L;
        }
        if(unit.startsWith("m")) {
            return sizeInt * 1024 * 1024L;
        }
        if(unit.startsWith("g")) {
            return sizeInt * 1024 * 1024 * 1024L;
        }

        return sizeInt;

    }





} // E:O:F:I18NUtil.
