package com.dotmarketing.util;

import com.liferay.portal.model.User;


/**
 * The AdminLogger class is used to log the large scale admin actions
 * to admin-audit.log
 * 
 */

public class AdminLogger {
	
	public static void log(Class cl,String methodName,String msg){		
		Logger.info(cl,  methodName + " : " + msg);
		Logger.info(AdminLogger.class, cl.toString() + " : " + methodName + " : " + msg);
	}
	
	public static void log(Class cl,String methodName,String msg,User user){
		if(user == null || user.getUserId() == null){
			log(cl, methodName, msg);
		}else{
			Logger.info(cl, "UserId : "+user.getUserId()+ " : " + methodName + " : " + msg);
			Logger.info(AdminLogger.class,"UserId : "+user.getUserId()+ " : " + cl.toString() + " : " + methodName + " : " + msg);
		}
	}

}
