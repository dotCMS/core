package com.dotmarketing.loggers.mbeans;

public interface Log4jConfigMBean {

	public abstract void enableInfo(String target);

	public abstract void enableWarn(String target);

	public abstract void enableError(String target);

	public abstract void enableDebug(String target);

}