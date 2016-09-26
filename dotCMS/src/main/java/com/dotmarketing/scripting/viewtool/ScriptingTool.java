package com.dotmarketing.scripting.viewtool;

import org.apache.velocity.tools.view.tools.ViewTool;

public interface ScriptingTool extends ViewTool {

	/**
	 * Sets global variables
	 * @param key
	 * @param val
	 */
	public void set(String key, Object val) ;

	/**
	 * Will evaluate a file OR passed in scriptlet executing the last expression if there is one in the file  
	 * @param scriptletOrFile
	 * @return
	 */
	public String eval(String scriptletOrFile);
	
	/**
	 * Similar to the eval except this can return an actual scripting object to be passed to the call method.
	 * @param File - Can only be used on an actual php file
	 * @param evalExpression
	 * @return
	 */
	public String evalExpression(String file, String evalExpression);

}
