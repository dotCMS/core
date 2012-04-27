package com.dotcms.achecker.utility;


import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.dotcms.achecker.dao.LanguageTextDAO;
import com.dotcms.achecker.model.LanguageTextBean;

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
