package com.dotcms.rendering.js;

public interface JsViewTool {

    String getName ();

    default SCOPE getScope () {
        return SCOPE.REQUEST;
    }

    enum SCOPE {
        REQUEST,
        APPLICATION
    }
}
