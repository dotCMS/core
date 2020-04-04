/*
 *  UtilMethods.java
 *
 *  Created on March 4, 2002, 2:56 PM
 */
package com.dotmarketing.util;

import java.io.File;
import java.util.function.Supplier;
import org.apache.velocity.servlet.VelocityServlet;
import org.apache.velocity.tools.view.tools.ViewTool;
import org.slf4j.LoggerFactory;
import com.dotcms.business.expiring.ExpiringMap;
import com.dotcms.business.expiring.ExpiringMapBuilder;
import com.dotmarketing.loggers.Log4jUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Objects;

/**
 *@author     David Torres
 */
public class Logger{

	static {
		Log4jUtil.configureDefaultSystemProperties();
	}
	private static Cache<Class, org.slf4j.Logger> map = Caffeine.newBuilder().maximumSize(10000l).build();

	public static void clearLoggers(){
		map.invalidateAll();
	}

    public static org.slf4j.Logger clearLogger ( Class clazz ) {
        try {
            return map.getIfPresent(clazz );
        }
        finally {
            map.invalidate(clazz);
        }
	}
	

	/**
	 * This class is syncrozned.  It shouldn't be called. It is exposed so that 
	 * @param cl
	 * @return
	 */
	private static org.slf4j.Logger loadLogger(final Class cl){
	    return map.get(cl, k->LoggerFactory.getLogger(k));
		

	}

	public static void info(Class clazz, final Supplier<String> message) {
		if (isInfoEnabled(clazz)) {
			info(clazz, message.get());
		}
	}

	public static void info(final Object ob, final Supplier<String> message) {
		if (isInfoEnabled(ob.getClass())) {
			info(ob.getClass(), message.get());
		}
	}

    public static void info(Object ob, String message) {
        info(ob.getClass(), message);
    }

    public static void info(Class cl, String message) {
    	if(isVelocityMessage(cl)){
    		velocityInfo(cl, message);
    		return;
    	}
    	final org.slf4j.Logger logger = loadLogger(cl);  
        logger.info(message);
    }

	public static void debug(final Object ob, final Supplier<String> message) {
		if (isDebugEnabled(ob.getClass())) {
			debug(ob.getClass(), message.get());
		}
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
    	final org.slf4j.Logger logger = loadLogger(cl);  
        logger.debug(message);
    }

    public static void debug(Class cl, String message, Throwable ex) {
    	if(isVelocityMessage(cl)){
    		velocityDebug(cl, message, ex);
    		return;
    	}
    	final org.slf4j.Logger logger = loadLogger(cl);  
        logger.debug(message, ex);
    }
    
    public static void error(Object ob, Throwable ex) {
      error(ob.getClass(), ex.getMessage(), ex);
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
    	final org.slf4j.Logger logger = loadLogger(cl);  

        logger.error(message);
    }

    public static void error(Class cl, String message, Throwable ex) {
    	if(isVelocityMessage(cl)){
    		velocityError(cl, message, ex);
    		return;
    	}
    	final org.slf4j.Logger logger = loadLogger(cl);  
    	try{
    	    logger.error(message, ex);
    	}
    	catch(java.lang.IllegalStateException e){
    	    ex.printStackTrace();
    	}
    }
    /**
     * a map with a 5 minute max lifespan
     */
    static ExpiringMap<Long, Long> logMap = new ExpiringMapBuilder().ttl(600*1000).size(2000).build();

	/**
	 * this method will print the message at WARN level every millis set
	 * and print the message plus whole stack trace if at DEGUG level
	 * @param cl
	 * @param message
	 * @param ex
	 * @param warnEveryMillis
	 */
	public static void warnEveryAndDebug(final Class cl, final String message, final Throwable ex, final int warnEveryMillis) {
        if(ex==null) {
			return;
		}
        final org.slf4j.Logger logger = loadLogger(cl);    
        final StackTraceElement ste = ex.getStackTrace()[0];

        final Long hash=new Long(Objects.hashCode(ste,message.substring(0, Math.min(message.length(), 10)),cl));
        final Long expireWhen = logMap.get(hash);
        
        if(expireWhen == null || expireWhen < System.currentTimeMillis()) {
            logMap.put(hash, System.currentTimeMillis() + warnEveryMillis );
            logger.warn(message + " (every "+warnEveryMillis + " millis)");
            
        }
        logger.debug(message, ex);
    }
    

    /**
     * this method will print the message at WARN level every millis set
     * and print the message plus whole stack trace if at DEGUG level
     * @param cl
     * @param message
     * @param ex
     * @param warnEveryMillis
     */
    public static void infoEvery(final Class cl, final String message, final int warnEveryMillis) {

        final org.slf4j.Logger logger = loadLogger(cl);    


        final Long hash=new Long(Objects.hashCode(cl.getName(),message.substring(0, Math.min(message.length(), 10)),cl));
        final Long expireWhen = logMap.get(hash);
        
        if(expireWhen == null || expireWhen < System.currentTimeMillis()) {
            logMap.put(hash, System.currentTimeMillis() + warnEveryMillis );
            logger.info(message + " (every "+warnEveryMillis + " millis)");
            
        }
        logger.debug(message);
    }
	
	
	

    public static void warnEveryAndDebug(final Class cl, final Throwable ex, final int ttlMillis) {
        warnEveryAndDebug(cl,ex.getMessage(), ex, ttlMillis );
    }
    
    
    
    /**
     * this method will print the message if at the WARN level
     * and print the message + whole stack trace if at the DEGUG LEVEL
     * @param cl
     * @param message
     * @param ex
     */
    public static void warnAndDebug(Class cl, String message, Throwable ex) {
        final org.slf4j.Logger logger = loadLogger(cl);    
        try{
            logger.warn(message);
            logger.debug(message, ex);
        }
        catch(java.lang.IllegalStateException e){
            ex.printStackTrace();
        }
    }

    /**
     * this method will print the message if at the WARN level
     * and print the message + whole stack trace if at the DEGUG LEVEL
     * @param cl
     * @param message
     * @param ex
     */
    public static void warnAndDebug(Class cl, Throwable ex) {
        warnAndDebug(cl, ex.getMessage(), ex);
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
    	final org.slf4j.Logger logger = loadLogger(cl);    
        logger.error("FATAL " + message);
    }

    public static void fatal(Class cl, String message, Throwable ex) {
    	if(isVelocityMessage(cl)){
    		velocityFatal(cl, message, ex);
    		return;
    	}
    	final org.slf4j.Logger logger = loadLogger(cl);    
        logger.error("FATAL " + message, ex);
    }

	public static void warn(final Object ob, final Supplier<String> message) {
		if (isWarnEnabled(ob.getClass())) {
			warn(ob.getClass(), message.get());
		}
	}

	public static void warn(Class clazz, final Supplier<String> message) {
		if (isWarnEnabled(clazz)) {
			warn(clazz, message.get());
		}
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

    	final org.slf4j.Logger logger = loadLogger(cl);    

        logger.warn(message);
    }

    public static void warn(Class cl, String message, Throwable ex) {
    	if(isVelocityMessage(cl)){
    		velocityWarn(cl, message, ex);
    		return;
    	}

    	final org.slf4j.Logger logger = loadLogger(cl);    

        logger.warn(message, ex);
    }
    public static boolean isDebugEnabled(Class cl) {
        final org.slf4j.Logger logger = loadLogger(cl); 
        return logger.isDebugEnabled();

    }

    public static boolean isInfoEnabled(Class cl) {
        final org.slf4j.Logger logger = loadLogger(cl); 

        return logger.isInfoEnabled();

    }
    public static boolean isWarnEnabled(Class cl) {
        final org.slf4j.Logger logger = loadLogger(cl); 

        return logger.isWarnEnabled();

    }
    public static boolean isErrorEnabled(Class cl) {
    	final org.slf4j.Logger logger = loadLogger(cl); 
        return logger.isErrorEnabled();

    }
    

    
    
    
    public static void velocityError(Class cl, String message, Throwable thr){
        final org.slf4j.Logger logger = loadLogger(cl); 
		logger.error(message + " @ " +  Thread.currentThread().getName(), thr);
    }
    
    public static void velocityWarn(Class cl, String message, Throwable thr){
        final org.slf4j.Logger logger = loadLogger(cl); 
		logger.warn(message + " @ " +  Thread.currentThread().getName(), thr);
    }
    
    public static void velocityInfo(Class cl, String message, Throwable thr){
    	final org.slf4j.Logger logger = loadLogger(cl); 
		logger.info(message + " @ " +  Thread.currentThread().getName(), thr);
    }
    public static void velocityFatal(Class cl, String message, Throwable thr){
        final org.slf4j.Logger logger = loadLogger(cl); 
		logger.error(message + " @ " +  Thread.currentThread().getName(), thr);
	}
    public static void velocityDebug(Class cl, String message, Throwable thr){
        final org.slf4j.Logger logger = loadLogger(cl); 
		logger.debug(message + " @ " +   Thread.currentThread().getName(), thr);
	}

    public static void velocityError(Class cl, String message){
        final org.slf4j.Logger logger = loadLogger(cl); 
		logger.error(message + " @ " +  Thread.currentThread().getName());
	}
	
	public static void velocityWarn(Class cl, String message){
	    final org.slf4j.Logger logger = loadLogger(cl); 
		logger.warn(message + " @ " +  Thread.currentThread().getName());
	}
	
	public static void velocityInfo(Class cl, String message){
	    final org.slf4j.Logger logger = loadLogger(cl); 
		logger.info(message + " @ " +  Thread.currentThread().getName());
	}
	public static void velocityFatal(Class cl, String message){
	    final org.slf4j.Logger logger = loadLogger(cl); 
		logger.error("FATAL " + message + " @ " +  Thread.currentThread().getName());
	}
	public static void velocityDebug(Class cl, String message){
	    final org.slf4j.Logger logger = loadLogger(cl); 
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
