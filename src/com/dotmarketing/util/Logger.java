/*
 *  UtilMethods.java
 *
 *  Created on March 4, 2002, 2:56 PM
 */
package com.dotmarketing.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Level;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.context.InternalContextAdapterImpl;

import com.dotmarketing.beans.Host;
import com.dotmarketing.velocity.VelocityServlet;


/**
 *@author     David Torres
 */
public class Logger{

	private static Map<Class, org.apache.log4j.Logger> map = new HashMap<Class, org.apache.log4j.Logger>();

	
	/**
	 * This class is syncrozned.  It shouldn't be called. It is exposed so that 
	 * @param cl
	 * @return
	 */
	private synchronized static org.apache.log4j.Logger loadLogger(Class cl){
		if(map.get(cl) == null){
			org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(cl);
			map.put(cl, logger);
		}
		return map.get(cl);
	}

    public static void info(Object ob, String message) {
        Class cl = ob.getClass();
        org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.info(message);
    }

    public static void info(Class cl, String message) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.info(message);
    }

    public static void debug(Object ob, String message) {
        Class cl = ob.getClass();
        org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.debug(message);
    }

    public static void debug(Object ob, String message, Throwable ex) {
        Class cl = ob.getClass();
        org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.debug(message, ex);
    }

    public static void debug(Class cl, String message) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.debug(message);
    }

    public static void debug(Class cl, String message, Throwable ex) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.debug(message, ex);
    }

    public static void error(Object ob, String message) {
        Class cl = ob.getClass();
        org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
    	velocityLogError(cl);
        logger.error(message);
    }

    public static void error(Object ob, String message, Throwable ex) {
        Class cl = ob.getClass();
        org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
    	velocityLogError(cl);
        logger.error(message, ex);
    }

    public static void error(Class cl, String message) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
    	velocityLogError(cl);
        logger.error(message);
    }

    public static void error(Class cl, String message, Throwable ex) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
    	velocityLogError(cl);
        logger.error(message, ex);
    }

    public static void fatal(Object ob, String message) {
        Class cl = ob.getClass();
        org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
    	
        logger.fatal(message);
    }

    public static void fatal(Object ob, String message, Throwable ex) {
        Class cl = ob.getClass();
        org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.fatal(message, ex);
    }

    public static void fatal(Class cl, String message) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.fatal(message);
    }

    public static void fatal(Class cl, String message, Throwable ex) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.fatal(message, ex);
    }

    public static void warn(Object ob, String message) {
        Class cl = ob.getClass();
        org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.warn(message);
    }

    public static void warn(Object ob, String message, Throwable ex) {
        Class cl = ob.getClass();
        org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.warn(message, ex);
    }

    public static void warn(Class cl, String message) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.warn(message);
    }

    public static void warn(Class cl, String message, Throwable ex) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        logger.warn(message, ex);
    }
    public static boolean isDebugEnabled(Class cl) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        return logger.isEnabledFor(Level.DEBUG);
//    	return false;
    }

    public static boolean isInfoEnabled(Class cl) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        return logger.isEnabledFor(Level.INFO);
//    	return false;
    }
    public static boolean isWarnEnabled(Class cl) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        return logger.isEnabledFor(Level.WARN);
//    	return false;
    }
    public static boolean isErrorEnabled(Class cl) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        return logger.isEnabledFor(Level.ERROR);
//    	return false;
    }
    
    public static org.apache.log4j.Logger getLogger(Class cl) {
    	org.apache.log4j.Logger logger = map.get(cl);
    	if(logger == null){
    		logger = loadLogger(cl);	
    	}
        return logger;
    }
    
    
    
    private static void velocityLogError(Class cl){
    	if(VelocityServlet.velocityCtx.get() != null){
    		Context ctx =  VelocityServlet.velocityCtx.get();
    		InternalContextAdapter ica =  new InternalContextAdapterImpl(ctx);
    		org.apache.log4j.Logger logger = map.get(VelocityServlet.class);
    		logger.error("#--------------------------------------------------------------------------------------");
    		logger.error("#");
    		if(ica.getCurrentMacroName() != null){
    			logger.error("# Velocity Error");
    		}

    		if(ctx.get("VTLSERVLET_URI") != null && ctx.get("host") != null ){
    			logger.error("# on url      : " + ((Host) ctx.get("host")).getHostname()  + ctx.get("VTLSERVLET_URI") );
    		}
    		else if(ctx.get("VTLSERVLET_URI") != null ){
    			logger.error("# on uri      : " + ctx.get("VTLSERVLET_URI") );
    		}
    		else if(ctx.get("host") != null){
    			logger.error("# on host     : " + ((Host) ctx.get("host")).getHostname() );
    		}
    		if(ctx.get("request") != null){
    			HttpServletRequest req  = (HttpServletRequest)ctx.get("request");
    			if(req.getAttribute("javax.servlet.forward.request_uri") != null){
    				logger.error("# on req      : " + req.getAttribute("javax.servlet.forward.request_uri") );
    			}
    			
     		}
    		if(ica.getCurrentMacroName() != null){
    			logger.error("# with macro  : #" + ica.getCurrentMacroName());
    		}
    		if(ica.getCurrentTemplateName() != null){
    			logger.error("# on template : " + normalizeTemplate(ica.getCurrentTemplateName()));
    		}
    		logger.error("#    stack:");
    		for(Object obj : ica.getTemplateNameStack()){
				logger.error("#    -- " + normalizeTemplate(obj));
    		}
    		logger.error("#");
    		logger.error("#--------------------------------------------------------------------------------------");
    	}
    	
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
    
    
}
