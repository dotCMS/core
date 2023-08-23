package com.dotcms.cli.common;

public enum InputOutputFormat {

    JSON("json"),
    YAML("yml"),
    YML("yml");

    String extension;

    InputOutputFormat(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    public static InputOutputFormat defaultFormat(){ return JSON; }
}
