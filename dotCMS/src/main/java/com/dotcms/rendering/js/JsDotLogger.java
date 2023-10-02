package com.dotcms.rendering.js;

import com.dotmarketing.util.Logger;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

/**
 * Encapsulates the dotcms logging services
 * @author jsanca
 */
public class JsDotLogger implements Serializable {

    @HostAccess.Export
    public void info(final String message) {
        Logger.info(this, message);
    }

    @HostAccess.Export
    public void debug(final String message) {
        Logger.debug(this, message);
    }

    @HostAccess.Export
    public void error(final String message) {
        Logger.error(this, message);
    }

    @HostAccess.Export
    public void error(final String message, final Object error) {
        if (error instanceof Throwable) {
            Logger.error(this, message, Throwable.class.cast(error));
        } else {
            Logger.error(this, message + ", error: " + error);
        }
    }

    @HostAccess.Export
    public void warn(final String message) {
        Logger.warn(this, message);
    }
}
