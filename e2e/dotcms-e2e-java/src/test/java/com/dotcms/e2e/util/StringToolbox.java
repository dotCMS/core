package com.dotcms.e2e.util;

public class StringToolbox {

    private StringToolbox() {
    }

    public static String camelToSnake(String str) {
        return str.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

}
