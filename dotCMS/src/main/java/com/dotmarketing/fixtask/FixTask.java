package com.dotmarketing.fixtask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;

public interface FixTask {
	
	 /**
	  * Execute a high performance query to determine if fix needs to be run
	  */
	boolean shouldRun();
	
	/**
	 * The instructions to execute.
	 * @throws DotDataException
	 * @throws DotRuntimeException
	 */
	List <Map <String,Object>>executeFix() throws DotDataException, DotRuntimeException;
	
	
	 /**
	 * Get List to save as XML to the directory backups/fixdata with file name of datetime and fixtask that was run 
	 */
	List <Map<String, String>> getModifiedData() ;
	 
	
}
