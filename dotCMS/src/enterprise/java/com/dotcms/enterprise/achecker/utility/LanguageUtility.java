/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

package com.dotcms.enterprise.achecker.utility;


import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.dotcms.enterprise.achecker.dao.LanguageTextDAO;
import com.dotcms.enterprise.achecker.model.LanguageTextBean;
import com.dotmarketing.util.Logger;

public class LanguageUtility {
	
	private static final String DEFAULT_LANGUAGE = "eng";
	
	private static Map<String, String> stringCache = new HashMap<>();
	
	private static LanguageTextDAO dao;

	/**
	 * Creates the shared {@link LanguageTextDAO} on first use. A failure is logged and leaves the
	 * DAO unset, so the next call tries again.
	 */
	private static void ensureDAO() {
		if ( dao == null ) {
			try {
				dao = new LanguageTextDAO();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
				Logger.error(LanguageUtility.class, "Accessibility Checker language texts are not available: "
						+ e.getClass().getName());
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

	/**
	 * Returns the text of the given term in the given language. Texts that are found are cached.
	 * When the term has no text in that language, or the lookup fails, the term itself is
	 * returned so callers always get something to display.
	 *
	 * @param name the language term
	 * @param lang the language code; {@code en} is read as {@code eng} and {@code null} means
	 *             {@code eng}
	 * @return the localized text, or {@code name} when none is found; {@code null} only when
	 *         {@code name} is {@code null}
	 */
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
			Logger.error(LanguageUtility.class, "Accessibility Checker language text could not be read: "
					+ e.getClass().getName());
		}

		if ( returnValue != null )
			stringCache.put(name + "-" + lang, returnValue);
		
		if ( returnValue == null )
			return name;
		
		return returnValue;
	}

}
