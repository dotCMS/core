package com.dotcms.cli.common;

public enum InputOutputFormat {
    JSON, YAML, YML;
    public static InputOutputFormat defaultFormat(){ return JSON; }
}
