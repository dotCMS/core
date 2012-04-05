package com.eng.achecker.utility;

/************************************************************************/
/* ACheckerImplImpl                                                             */
/************************************************************************/
/* Copyright (c) 2008 - 2011                                            */
/* Inclusive Design Institute                                           */
/*                                                                      */
/* This program is free software. You can redistribute it and/or        */
/* modify it under the terms of the GNU General Public License          */
/* as published by the Free Software Foundation.                        */
/************************************************************************/
// $Id$

/**
* CheckFuncUtility.class.php
* Utility class for check functions
*
* @access	public
* @author	Cindy Qi Li
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

	/**
	* Convert php code into script that is run by eval() from AccessibilityValidator.class.php
	* @access  public
	* @param   $code
	* @return  true: correct syntax; 
	*          false: wrong syntax
	* @author  Cindy Qi Li
	*/
	public static String convertCode(String code) 
	{
		return code;
		/*
		if (trim($code) == '')
    		return 'return true;';
    	else
			return 'global $global_e, $global_content_dom, $header_array, $base_href, $global_check_id, $htmlValidator, $uri; $global_e = $e; $global_content_dom = $this->content_dom; $uri = $this->uri; $global_check_id=$check_id; ' . $code;
		*/
	}
}

