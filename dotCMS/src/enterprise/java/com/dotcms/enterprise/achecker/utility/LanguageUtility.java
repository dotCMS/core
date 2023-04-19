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


import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.dotcms.enterprise.achecker.dao.LanguageTextDAO;
import com.dotcms.enterprise.achecker.model.LanguageTextBean;

public class LanguageUtility {
	
	private static final String DEFAULT_LANGUAGE = "eng";
	
	private static Map<String, String> stringCache = new HashMap<String, String>();
	
	private static LanguageTextDAO dao;

	private static void ensureDAO() {
		if ( dao == null ) {
			try {
				dao = new LanguageTextDAO();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static String _AC(String name) {
		return _AC(name, DEFAULT_LANGUAGE);
	}
	
	private static String mapWebLanguages(String lang) {
		if (lang == null)
			return lang;
		if ( lang.equals("en"))
			return "eng";
		return lang;
	}

	public static String _AC(String name, String lang) {

		if ( name == null )
			return null;
		
		if ( lang == null )
			lang = DEFAULT_LANGUAGE;
		
		lang = mapWebLanguages(lang);
		
		String result = stringCache.get(name + "-" + lang);
		if ( result != null )
			return result;
		
		ensureDAO();
		LanguageTextBean row;
		String returnValue = null;
		try {
			row = dao.getByTermAndLang(name, lang);
			if ( row != null ) {
				returnValue =  row.getTextString();				 
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if ( returnValue != null )
			stringCache.put(name + "-" + lang, returnValue);
		
		if ( returnValue == null )
			return name;
		
		return returnValue;
	}

}
