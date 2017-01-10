package com.dotmarketing.util;

import com.liferay.portal.model.User;

public class LuceneProfiler {

	public static void log(Class cl,String msg){		
		Logger.info(LuceneProfiler.class, cl.toString() + "ThreadID = " + 
			Thread.currentThread().getId() + " ThreadName = " + Thread.currentThread().getName() + " " + msg);
	}
	
	public static void log(Class cl,String methodName,String msg){		
		Logger.info(LuceneProfiler.class, cl.toString() + " : " + methodName + " : " + "ThreadID = " + 
				Thread.currentThread().getId() + " ThreadName = " + Thread.currentThread().getName() + " " + msg);
	}
	
	public static void log(Class cl,String methodName,String msg,User user){
		if(user == null || user.getUserId() == null){
			log(cl, methodName, msg);
		}else{
			Logger.info(LuceneProfiler.class,  cl.toString() + " UserId : "+user.getUserId()+ " : " + methodName + " : " + "ThreadID = " + 
					Thread.currentThread().getId() + " ThreadName = " + Thread.currentThread().getName() + " " + msg);
		}
	}
	
}
