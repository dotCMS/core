package com.dotmarketing.loggers.mbeans;

import org.apache.log4j.Level;

import com.dotmarketing.util.Logger;

public class Log4jConfig implements Log4jConfigMBean  {

	/* (non-Javadoc)
	 * @see com.dotmarketing.loggers.Log4jConfigMBean#enableInfo(java.lang.String)
	 */
	public void enableInfo(String target) {
		try {
			Class c = Class.forName(target);
			if (c != null) {
				Logger.getLogger(c).setLevel(Level.INFO);
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
			if (c != null) {
				Logger.getLogger(c).setLevel(Level.WARN);
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
			if (c != null) {
				Logger.getLogger(c).setLevel(Level.ERROR);
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
			if (c != null) {
				Logger.getLogger(c).setLevel(Level.DEBUG);
			}
		} catch (ClassNotFoundException e) {
		}
	}

}
