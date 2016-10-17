/*
 *  UtilMethods.java
 *
 *  Created on March 4, 2002, 2:56 PM
 */
package com.dotmarketing.util;

import java.io.File;
import java.util.WeakHashMap;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotcms.repackage.org.apache.logging.log4j.LogManager;
import com.dotmarketing.velocity.VelocityServlet;

/**
 *@author     David Torres
 */
public class Logger{

	private static WeakHashMap<Class, com.dotcms.repackage.org.apache.logging.log4j.Logger> map = new WeakHashMap<>();

	public static void clearLoggers(){
		map.clear();
	}

    public static com.dotcms.repackage.org.apache.logging.log4j.Logger clearLogger ( Class clazz ) {
        return map.remove( clazz );
	}
	

	/**
	 * This class is syncrozned.  It shouldn't be called. It is exposed so that 
	 * @param cl
	 * @return
	 */
	private synchronized static com.dotcms.repackage.org.apache.logging.log4j.Logger loadLogger(Class cl){
		if(map.get(cl) == null){
			com.dotcms.repackage.org.apache.logging.log4j.Logger logger = LogManager.getLogger(cl);
			map.put(cl, logger);
		}
		return map.get(cl);
	}

    public static void info(Object ob, String message) {
        info(ob.getClass(), message);
    }

    public static void info(Class cl, String message) {
    	if(isVelocityMessage(cl)){
    		velocityInfo(cl, message);
    		return;
    	}
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.info(message);
    }

    public static void debug(Object ob, String message) {
    	debug(ob.getClass(), message);
    }

    public static void debug(Object ob, String message, Throwable ex) {
    	debug(ob.getClass(), message,ex);
    }

    public static void debug(Class cl, String message) {
    	if(isVelocityMessage(cl)){
    		velocityDebug(cl, message);
    		return;
    	}
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.debug(message);
    }

    public static void debug(Class cl, String message, Throwable ex) {
    	if(isVelocityMessage(cl)){
    		velocityDebug(cl, message, ex);
    		return;
    	}
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.debug(message, ex);
    }

    public static void error(Object ob, String message) {
    	error(ob.getClass(), message);
    }

    public static void error(Object ob, String message, Throwable ex) {
    	error(ob.getClass(), message, ex);
    }

    public static void error(Class cl, String message) {
    	if(isVelocityMessage(cl)){
    		velocityError(cl, message);
    		return;
    	}
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}

        logger.error(message);
    }

    public static void error(Class cl, String message, Throwable ex) {
    	if(isVelocityMessage(cl)){
    		velocityError(cl, message, ex);
    		return;
    	}
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}

        logger.error(message, ex);
    }

    public static void fatal(Object ob, String message) {
    	fatal(ob.getClass(), message);
    }

    public static void fatal(Object ob, String message, Throwable ex) {
    	fatal(ob.getClass(), message, ex);
    }

    public static void fatal(Class cl, String message) {
    	if(isVelocityMessage(cl)){
    		velocityFatal(cl, message);
    		return;
    	}
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.fatal(message);
    }

    public static void fatal(Class cl, String message, Throwable ex) {
    	if(isVelocityMessage(cl)){
    		velocityFatal(cl, message, ex);
    		return;
    	}
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.fatal(message, ex);
    }

    public static void warn(Object ob, String message) {
    	warn(ob.getClass(), message);
    }

    public static void warn(Object ob, String message, Throwable ex) {
    	warn(ob.getClass(), message, ex);
    }

    public static void warn(Class cl, String message) {
    	if(isVelocityMessage(cl)){
    		velocityWarn(cl, message);
    		return;
    	}
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}

        logger.warn(message);
    }

    public static void warn(Class cl, String message, Throwable ex) {
    	if(isVelocityMessage(cl)){
    		velocityWarn(cl, message, ex);
    		return;
    	}
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}

        logger.warn(message, ex);
    }
    public static boolean isDebugEnabled(Class cl) {
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        return logger.isDebugEnabled();

    }

    public static boolean isInfoEnabled(Class cl) {
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        return logger.isInfoEnabled();

    }
    public static boolean isWarnEnabled(Class cl) {
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        return logger.isWarnEnabled();

    }
    public static boolean isErrorEnabled(Class cl) {
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        return logger.isErrorEnabled();

    }
    
    public static com.dotcms.repackage.org.apache.logging.log4j.Logger getLogger(Class cl) {
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        return logger;
    }
    
    
    
    public static void velocityError(Class cl, String message, Throwable thr){
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
		logger.error(message + " @ " +  Thread.currentThread().getName(), thr);
    }
    
    public static void velocityWarn(Class cl, String message, Throwable thr){
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
		logger.warn(message + " @ " +  Thread.currentThread().getName(), thr);
    }
    
    public static void velocityInfo(Class cl, String message, Throwable thr){
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
		logger.info(message + " @ " +  Thread.currentThread().getName(), thr);
    }
    public static void velocityFatal(Class cl, String message, Throwable thr){
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
		logger.fatal(message + " @ " +  Thread.currentThread().getName(), thr);
	}
    public static void velocityDebug(Class cl, String message, Throwable thr){
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
		logger.debug(message + " @ " +   Thread.currentThread().getName(), thr);
	}

    public static void velocityError(Class cl, String message){
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
		logger.error(message + " @ " +  Thread.currentThread().getName());
	}
	
	public static void velocityWarn(Class cl, String message){
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
		logger.warn(message + " @ " +  Thread.currentThread().getName());
	}
	
	public static void velocityInfo(Class cl, String message){
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
		logger.info(message + " @ " +  Thread.currentThread().getName());
	}
	public static void velocityFatal(Class cl, String message){
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
		logger.fatal(message + " @ " +  Thread.currentThread().getName());
	}
	public static void velocityDebug(Class cl, String message){
    	com.dotcms.repackage.org.apache.logging.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
		logger.debug(message + " @ " +  Thread.currentThread().getName());
	}
    
    
    
    private static String normalizeTemplate(Object t){
    	if(t ==null){
    		return null;
    	}
		String x = t.toString();
		x = x.replace(File.separatorChar, '/');
		x = (x.indexOf("assets") > -1) ? x.substring(x.lastIndexOf("assets"), x.length()) : x;
		x = (x.startsWith("/")) ?  x : "/" + x;

		return x;
    }
    
    
    private static boolean isVelocityMessage(Object obj){
    	if(obj==null)return false;
    	return isVelocityMessage(obj.getClass());
    }
    
    private static boolean isVelocityMessage(Class clazz){
    	boolean ret = false;
    	if(clazz!=null && clazz.getName()!=null){
        	String name = clazz.getName().toLowerCase();
        	ret= name.contains("velocity") || name.contains("viewtool");

        	if(!ret){
					ret= ViewTool.class.isAssignableFrom(clazz);
        	}
        	if(!ret){
					ret= VelocityServlet.class.isAssignableFrom(clazz);
        	}
    	}
    	return ret;

    }
}
