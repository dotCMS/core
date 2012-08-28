package com.dotmarketing.util;

import com.dotmarketing.logConsole.model.LogMapper;


public class ActivityLogger {

    private static String filename = "dotcms-useractivity.log";

    public static synchronized void logInfo ( Class cl, String action, String msg, String host ) {

        if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
            Logger.info( ActivityLogger.class, cl.toString() + ": " + host + " : " + action + " , " + msg );
        }
    }

    public static void logDebug ( Class cl, String action, String msg, String host ) {

        if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
            Logger.debug( ActivityLogger.class, cl.toString() + ": " + host + " :" + action + " , " + msg );
        }
    }
}
