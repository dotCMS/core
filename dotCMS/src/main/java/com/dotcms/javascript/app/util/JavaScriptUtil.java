package com.dotcms.javascript.app.util;

import com.dotcms.config.DotInitializer;
import com.dotcms.javascript.app.JavascriptEngine;


public class JavaScriptUtil implements DotInitializer {

    private final static JavascriptEngine ENGINE = new JavascriptEngine();
    private final static Console CONSOLE = new Console();

    public static JavascriptEngine getEngine() {
        return ENGINE;
    }

    public static Console getConsole() {
        return CONSOLE;
    }

    @Override
    public void init() {

        // todo: add here global objects
    }
}
