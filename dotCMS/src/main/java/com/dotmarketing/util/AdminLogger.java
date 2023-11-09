package com.dotmarketing.util;

import com.dotmarketing.logConsole.model.LogMapper;
import com.liferay.portal.model.User;


/**
 * The AdminLogger class is used to log the large scale admin actions to admin-audit.log
 */
public class AdminLogger {

    private static String filename = "dotcms-adminaudit.log";
    private static String logType = "[Admin Audit] ";

    public static void log ( Class cl, String methodName, String msg ) {

        if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
            Logger.info( cl, logType + methodName + " : " + msg );
            Logger.info( AdminLogger.class, logType + cl.toString() + " : " + methodName + " : " + msg );
        }
    }

    public static void log ( Class cl, String methodName, String msg, User user ) {

        if ( user == null || user.getUserId() == null ) {
            log( cl, methodName, msg );
        } else {
            if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
                Logger.info( cl, logType + "UserId : " + user.getUserId() + " : " + methodName + " : " + msg );
                Logger.info( AdminLogger.class, logType + "UserId : " + user.getUserId() + " : " + cl.toString() + " : " + methodName + " : " + msg );
            }
        }
    }

}
