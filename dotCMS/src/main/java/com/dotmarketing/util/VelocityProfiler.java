package com.dotmarketing.util;

import com.liferay.portal.model.User;

public class VelocityProfiler {

	public static void log(Class cl,String msg){		
		Logger.info(cl,  msg);
		Logger.info(VelocityProfiler.class, cl.toString() + msg);
	}
	
	public static void log(Class cl,String methodName,String msg){		
		Logger.info(cl,  methodName + " : " + msg);
		Logger.info(VelocityProfiler.class, cl.toString() + " : " + methodName + " : " + msg);
	}
	
	public static void log(Class cl,String methodName,String msg,User user){
		if(user == null || user.getUserId() == null){
			log(cl, methodName, msg);
		}else{
			Logger.info(cl, "UserId : "+user.getUserId()+ " : " + methodName + " : " + msg);
			Logger.info(VelocityProfiler.class,"UserId : "+user.getUserId()+ " : " + cl.toString() + " : " + methodName + " : " + msg);
		}
	}
	
}
