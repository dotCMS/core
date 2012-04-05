package com.dotmarketing.util;

import org.apache.log4j.FileAppender;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.appender.ActiveAsynchronousAppender;
import com.dotmarketing.logConsole.model.LogMapper;


public class ActivityLogger {

	private static String filename = "dotcms-userActivity.log";

	public static synchronized void logInfo(Class cl, String action, String msg, String host) {

		if(LogMapper.getInstance().isLogEnabled(filename)) {
			Logger.info(ActivityLogger.class, cl.toString() + ":"+host+": " + action + " , " + msg);
		}

	}

	public static void logDebug(Class cl, String action, String msg, String host) {
		if(LogMapper.getInstance().isLogEnabled(filename)) {
			Logger.debug(ActivityLogger.class, cl.toString() + ":"+host+":" + action + " , " + msg);
		}
	}
}
