package com.dotcms.rendering.js;

import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;


/**
 * Abstraction of the Response for the Javascript engine.
 * @author jsanca
 */
public class JsResponse implements Serializable {

    private final HttpServletResponse response;

    public JsResponse(final HttpServletResponse response) {
        this.response = response;
    }

    // todo: code it here
}
