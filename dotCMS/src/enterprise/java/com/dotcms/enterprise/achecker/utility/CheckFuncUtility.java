/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.utility;



/**
* CheckFuncUtility.class.php
* Utility class for check functions
*
* @access	public

* @package  checker
*/

public class CheckFuncUtility {
	
	/**
	* check syntax of the code that is used in eval()
	* @access  public
	* @param   $code
	* @return  true: correct syntax; 
	*          false: wrong syntax
	* @author  Cindy Qi Li
	*/
	public static String validateSyntax(String code) {
    	// return @eval('return true;' . $code);
		return code;
	}

	/**
	* check syntax of the code that is used in eval()
	* @access  public
	* @param   $code
	* @return  true: correct syntax; 
	*          false: wrong syntax
	* @author  Cindy Qi Li
	*/
	public static String validateSecurity(String code) 
	{
		return code;
		
		/*
    	global $msg;
    	
		// php functions can not be called in the code
		$called_php_func = '';
		
		$php_funcs = get_defined_functions();
		foreach($php_funcs as $php_section)
		{
			foreach ($php_section as $php_func)
			{
				if (preg_match('/'.preg_quote($php_func).'\s*\(/i', $code))
					$called_php_func .= $php_func.'(), ';
			}
		}
		
		if ($called_php_func <> '') $msg->addError(array('NO_PHP_FUNC', substr($called_php_func, 0, -2)));

    	// php super global variables cannot be used in the code
		$superglobals = array('$GLOBALS','$_SERVER', '$_GET', '$_POST', '$_FILES', 
                          '$_COOKIE', '$_SESSION', '$_REQUEST', '$_ENV');
	
		$called_php_global_vars = '';
		foreach($superglobals as $superglobal)
		{
			if (stristr($code, $superglobal))
				$called_php_global_vars .= $superglobal.', ';
		}
		
		if ($called_php_global_vars <> '') $msg->addError(array('NO_PHP_GLOBAL_VARS', substr($called_php_global_vars, 0, -2)));
		return;
		*/
	}

	public static String convertCode(String code) {
		return code;
	}

}
