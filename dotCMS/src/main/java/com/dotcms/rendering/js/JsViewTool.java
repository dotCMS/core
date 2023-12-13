package com.dotcms.rendering.js;

/**
 * This interface is used to define a JS View Tool.
 * @author jsanca
 */
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
