/**
 * 
 */
package com.dotmarketing.util;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;

/**
 * @author jasontesser
 *
 */
public class DotVelocityLogger implements LogChute {

	public void init(RuntimeServices arg0) throws Exception {}

	public boolean isLevelEnabled(int level) {
		switch (level)
        {
            case LogChute.DEBUG_ID:
                return Logger.isDebugEnabled(VelocityEngine.class);
            case LogChute.INFO_ID:
                return Logger.isInfoEnabled(VelocityEngine.class);
            case LogChute.TRACE_ID:
                    return Logger.isDebugEnabled(VelocityEngine.class);
            case LogChute.WARN_ID:
                return Logger.isWarnEnabled(VelocityEngine.class);
            case LogChute.ERROR_ID:
                return Logger.isErrorEnabled(VelocityEngine.class);
            default:
                return true;
        }
//		return false;
	}

	public void log(int level, String message) {
		switch (level)
        {
            case LogChute.WARN_ID:
                Logger.warn(VelocityEngine.class, message);
                break;
            case LogChute.INFO_ID:
                Logger.info(VelocityEngine.class,message);
                break;
            case LogChute.DEBUG_ID:
            	Logger.debug(VelocityEngine.class,message);
                break;
            case LogChute.TRACE_ID:
                Logger.debug(VelocityEngine.class, message);
                break;
            case LogChute.ERROR_ID:
            	Logger.error(VelocityEngine.class, message);
                break;
            default:
            	Logger.debug(VelocityEngine.class, message);
                break;
        }
	}

	public void log(int level, String message, Throwable t) {
		switch (level)
        {
            case LogChute.WARN_ID:
                Logger.warn(VelocityEngine.class,message, t);
                break;
            case LogChute.INFO_ID:
            	Logger.info(VelocityEngine.class, message);
                break;
            case LogChute.DEBUG_ID:
            	Logger.debug(VelocityEngine.class, message, t);
                break;
            case LogChute.TRACE_ID:
            	Logger.debug(VelocityEngine.class, message, t);
                break;
            case LogChute.ERROR_ID:
            	Logger.error(VelocityEngine.class, message, t);
                break;
            default:
            	Logger.debug(VelocityEngine.class, message, t);
                break;
        }
	}

}
