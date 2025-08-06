package com.dotmarketing.util;

import com.liferay.portal.model.User;



public class AdminLogger {

    private static String filename = "dotcms-adminaudit.log";

    public static void log ( Class cl, String methodName, String msg ) {

        SecurityLogger.logInfo(cl, msg);
    }

    public static void log ( Class cl, String methodName, String msg, User user ) {

        SecurityLogger.logInfo(cl, msg);
    }

}
