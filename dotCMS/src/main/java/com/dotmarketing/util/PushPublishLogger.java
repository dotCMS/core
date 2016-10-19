package com.dotmarketing.util;

import com.dotmarketing.logConsole.model.LogMapper;
import com.liferay.portal.model.User;


public class PushPublishLogger {

    private static String filename = "dotcms-pushpublish.log";

    @SuppressWarnings("rawtypes")
	public static void log ( Class cl, String msg ) {

        if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
            Logger.info( PushPublishLogger.class, cl.toString() + " : " + msg );
        }
    }

    @SuppressWarnings("rawtypes")
	public static void log ( Class cl, String msg, String bundleId ) {

        if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
            Logger.info( PushPublishLogger.class, cl.toString() + " : [BundleID: "+bundleId+"] " + msg );
        }
    }

    @SuppressWarnings("rawtypes")
	public static void log ( Class cl, String msg, String bundleId, User user ) {

        if ( user == null || user.getUserId() == null ) {
            log( cl, msg , bundleId);
        } else {
            if ( LogMapper.getInstance().isLogEnabled( filename ) ) {
                Logger.info( PushPublishLogger.class, cl.toString() + " : [BundleID: "+bundleId+"] " + msg + ", User: " + user.getUserId());
            }
        }
    }

}
