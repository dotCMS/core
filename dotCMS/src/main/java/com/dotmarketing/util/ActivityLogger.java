package com.dotmarketing.util;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.logConsole.model.LogMapper;

public class ActivityLogger {

	private static String filename = "dotcms-userActivity.log";

	public static synchronized void logInfo(Class cl, String action, String msg) {
		logInfo(cl, action, msg, null);
	}

	public static synchronized void logInfo(Class cl, String action,
			String msg, String hostNameOrId) {
		if (LogMapper.getInstance().isLogEnabled(filename)) {
			Logger.info(ActivityLogger.class, cl.toString() + ": " + getHostName(hostNameOrId)
					+ " : " + action + " , " + msg);
		}
	}

	public static void logDebug(Class cl, String action, String msg, String hostNameOrId) {

		if (LogMapper.getInstance().isLogEnabled(filename)) {
			Logger.debug(ActivityLogger.class, cl.toString() + ": " + getHostName(hostNameOrId)
					+ " :" + action + " , " + msg);
		}
	}
	
	private static String getHostName(String hostNameOrId){
		if (!UtilMethods.isSet(hostNameOrId) || "SYSTEM_HOST".equals(hostNameOrId)) {
			return "system";
		}
		Host h = new Host();
		try {
			h = APILocator.getHostAPI().findByName(hostNameOrId, APILocator.getUserAPI().getSystemUser(), false);
		} catch (Exception e) {}
		if(!UtilMethods.isSet(h) || !UtilMethods.isSet(h.getIdentifier())){
			try {
				h = APILocator.getHostAPI().find(hostNameOrId, APILocator.getUserAPI().getSystemUser(), false);
			} catch (Exception e1) {}
		}
		if(UtilMethods.isSet(h) && UtilMethods.isSet(h.getIdentifier()))
			return h.getHostname();
		return "";
	}

}
