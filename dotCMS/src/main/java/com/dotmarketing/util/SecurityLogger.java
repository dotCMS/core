package com.dotmarketing.util;

import com.dotmarketing.logConsole.model.LogMapper;

public class SecurityLogger {

    private static String filename = "dotcms-security.log";

    public static void logInfo ( Class cl, String msg ) {

        if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
            Logger.info(SecurityLogger.class, cl.toString() + " : " + msg);
        }
    }

    public static void logDebug ( Class cl, String msg ) {

        if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
            Logger.debug(SecurityLogger.class, cl.toString() + " : " + msg);
        }
    }

}