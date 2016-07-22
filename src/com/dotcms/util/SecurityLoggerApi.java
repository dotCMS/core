package com.dotcms.util;

import java.io.Serializable;

/**
 * Logger to report security stuff
 * @author jsanca
 */
public interface SecurityLoggerAPI extends Serializable {


    /**
     * Log an info security message
     * @param cl {@link Class}
     * @param msg {@link String}
     */
    public default void logInfo ( final Class cl, final String msg ) {

        com.dotmarketing.util.SecurityLogger.logInfo(cl, msg);
    }

    /**
     * Log a debug security message
     * @param cl {@link Class}
     * @param msg {@link String}
     */
    public default void logDebug ( final Class cl, final String msg ) {

        com.dotmarketing.util.SecurityLogger.logDebug(cl, msg);
    }
} // E:O:F:SecurityLoggerAPI.
