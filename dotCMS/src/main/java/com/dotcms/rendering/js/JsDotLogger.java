package com.dotcms.rendering.js;

import com.dotmarketing.util.Logger;

import java.io.Serializable;

/**
 * Encapsulates the dotcms logging services
 * @author jsanca
 */
public class JsDotLogger implements Serializable {

    public void info(final String message) {
        Logger.info(this, message);
    }

    public void debug(final String message) {
        Logger.debug(this, message);
    }

    public void error(final String message) {
        Logger.error(this, message);
    }

    public void error(final String message, final Object error) {
        if (error instanceof Throwable) {
            Logger.error(this, message, Throwable.class.cast(error));
        } else {
            Logger.error(this, message + ", error: " + error);
        }
    }

    public void warn(final String message) {
        Logger.warn(this, message);
    }
}
