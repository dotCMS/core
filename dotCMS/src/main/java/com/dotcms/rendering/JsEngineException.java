package com.dotcms.rendering;

import com.dotmarketing.exception.DotRuntimeException;

/**
 * Generic exception for the JS engine
 */
public class JsEngineException extends DotRuntimeException  {
    public JsEngineException(String message) {
        super(message);
    }

    public JsEngineException(Throwable cause) {
        super(cause);
    }

    public JsEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
