/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
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

