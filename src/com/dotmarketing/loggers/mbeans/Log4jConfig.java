package com.dotmarketing.loggers.mbeans;

import com.dotcms.repackage.org.apache.logging.log4j.Level;
import com.dotcms.repackage.org.apache.logging.log4j.LogManager;
import com.dotcms.repackage.org.apache.logging.log4j.core.Logger;
import com.dotmarketing.loggers.Log4jUtil;

public class Log4jConfig implements Log4jConfigMBean  {

	/* (non-Javadoc)
	 * @see com.dotmarketing.loggers.Log4jConfigMBean#enableInfo(java.lang.String)
	 */
	public void enableInfo(String target) {
		try {
			Class c = Class.forName(target);
			if ( c != null ) {
				//Changing the logger level
				Log4jUtil.setLevel((Logger) LogManager.getLogger(c), Level.INFO);
			}
		} catch (ClassNotFoundException e) {
			
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.loggers.Log4jConfigMBean#enableWarn(java.lang.String)
	 */
	public void enableWarn(String target) {
		try {
			Class c = Class.forName(target);
			if ( c != null ) {
				//Changing the logger level
				Log4jUtil.setLevel((Logger) LogManager.getLogger(c), Level.WARN);
			}
		} catch (ClassNotFoundException e) {
		}
	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.loggers.Log4jConfigMBean#enableError(java.lang.String)
	 */
	public void enableError(String target) {
		try {
			Class c = Class.forName(target);
			if ( c != null ) {
				//Changing the logger level
				Log4jUtil.setLevel((Logger) LogManager.getLogger(c), Level.ERROR);
			}
		} catch (ClassNotFoundException e) {
		}

	}

	/* (non-Javadoc)
	 * @see com.dotmarketing.loggers.Log4jConfigMBean#enableDebug(java.lang.String)
	 */
	public void enableDebug(String target) {
		try {
			Class c = Class.forName(target);
			if ( c != null ) {
				//Changing the logger level
				Log4jUtil.setLevel((Logger) LogManager.getLogger(c), Level.DEBUG);
			}
		} catch (ClassNotFoundException e) {
		}
	}

}
