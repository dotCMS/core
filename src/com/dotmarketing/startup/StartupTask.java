package com.dotmarketing.startup;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;

public interface StartupTask {
	
	/**
	 * By Default tasks only execute once.  If you have a task that needs to execute more then once use this method.
	 * @return
	 */
	boolean forceRun();
	
	/**
	 * The instructions to execute.
	 * @throws DotDataException
	 * @throws DotRuntimeException
	 */
	void executeUpgrade() throws DotDataException, DotRuntimeException;
	
}
