package com.dotcms.javascript.app.util;

import com.dotcms.config.DotInitializer;
import com.dotcms.javascript.app.JavascriptEngine;

public class JavaScriptUtil implements DotInitializer {

    private final static JavascriptEngine ENGINE = new JavascriptEngine();

    public static JavascriptEngine getEngine() {
        return ENGINE;
    }

    @Override
    public void init() {

        // todo: add here global objects
    }
}
