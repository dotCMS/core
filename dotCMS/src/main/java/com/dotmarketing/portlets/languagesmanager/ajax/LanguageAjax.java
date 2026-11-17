package com.dotmarketing.portlets.languagesmanager.ajax;

import com.dotcms.languagevariable.business.LanguageKeysAccess;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ajax.DwrUtil;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

@Deprecated(since = "24.05", forRemoval = true)
public class LanguageAjax {

	public LanguageAPI langAPI = APILocator.getLanguageAPI();

	/**
	 * Returns the general language keys of a language. Same access rule as
	 * {@link #getLanguageKeys(String, String)}.
	 *
	 * @param languageCode the language code, for example {@code en}
	 * @return the language keys
	 * @throws DotSecurityException if nobody is logged in, or the logged-in user has access to
	 *                              neither the Languages nor the Locales portlet
	 * @throws DotDataException     if the user's portlets cannot be loaded
	 * @throws PortalException      if the logged-in user cannot be resolved
	 * @throws SystemException      if the logged-in user cannot be resolved
	 */
	public List<LanguageKey> getLanguageKeys(String languageCode)
			throws DotSecurityException, DotDataException, PortalException, SystemException {
		return getLanguageKeys(languageCode, null);
	}

	/**
	 * Returns the language keys of a language and country. Only a user with access to the
	 * Languages or the Locales portlet can read them, checked by
	 * {@link LanguageKeysAccess#requireLanguagesOrLocalesPortlet} as for {@link #saveKeys}.
	 *
	 * @param languageCode the language code, for example {@code en}
	 * @param countryCode  the country code, for example {@code US}, or {@code null} for the
	 *                     general keys
	 * @return the language keys
	 * @throws DotSecurityException if nobody is logged in, or the logged-in user has access to
	 *                              neither the Languages nor the Locales portlet
	 * @throws DotDataException     if the user's portlets cannot be loaded
	 * @throws PortalException      if the logged-in user cannot be resolved
	 * @throws SystemException      if the logged-in user cannot be resolved
	 */
	public List<LanguageKey> getLanguageKeys(String languageCode, String countryCode)
			throws DotSecurityException, DotDataException, PortalException, SystemException {
		LanguageKeysAccess.requireLanguagesOrLocalesPortlet(DwrUtil.getLoggedInUser());
		return langAPI.getLanguageKeys(languageCode, countryCode);
	}

	/**
	 * Returns one page of the general language keys of a language. Same access rule as
	 * {@link #getPaginatedLanguageKeys(String, String, int, String)}.
	 *
	 * @param languageCode the language code, for example {@code en}
	 * @param page         the page number, starting at 1
	 * @return the counters row followed by the keys of the page
	 * @throws DotSecurityException if nobody is logged in, or the logged-in user has access to
	 *                              neither the Languages nor the Locales portlet
	 * @throws DotDataException     if the user's portlets cannot be loaded
	 * @throws PortalException      if the logged-in user cannot be resolved
	 * @throws SystemException      if the logged-in user cannot be resolved
	 */
	public List<Map<String,Object>> getPaginatedLanguageKeys(String languageCode,int page)
			throws DotSecurityException, DotDataException, PortalException, SystemException {
		return getPaginatedLanguageKeys(languageCode, null,page,"");
	}

	private List<LanguageKey> filterList(List<LanguageKey> list, String filter) {
	    List<LanguageKey> filtered=new ArrayList<>();
	    for(LanguageKey lk : list) {
	        if(lk.getKey().contains(filter) || lk.getValue().contains(filter)) {
	            filtered.add(lk);
	        }
	    }
	    return filtered;
 	}

	/**
	 * Returns one page of the language keys of a language and country, as shown by the language
	 * keys page: the first row holds the counters, the others one key each with its general and
	 * country values. Only a user with access to the Languages or the Locales portlet can read
	 * them, checked by {@link LanguageKeysAccess#requireLanguagesOrLocalesPortlet} as for
	 * {@link #saveKeys}.
	 *
	 * @param languageCode the language code, for example {@code en}
	 * @param countryCode  the country code, for example {@code US}
	 * @param page         the page number, starting at 1
	 * @param filter       text the key or one of its values must contain, or empty for all keys
	 * @return the counters row followed by the keys of the page
	 * @throws DotSecurityException if nobody is logged in, or the logged-in user has access to
	 *                              neither the Languages nor the Locales portlet
	 * @throws DotDataException     if the user's portlets cannot be loaded
	 * @throws PortalException      if the logged-in user cannot be resolved
	 * @throws SystemException      if the logged-in user cannot be resolved
	 */
	public List<Map<String,Object>> getPaginatedLanguageKeys(String languageCode, String countryCode,int page,String filter)
			throws DotSecurityException, DotDataException, PortalException, SystemException {

		LanguageKeysAccess.requireLanguagesOrLocalesPortlet(DwrUtil.getLoggedInUser());

		int keysPerPage = 50;
		int keyIndex = 1;
		List<Map<String,Object>> result = new ArrayList<>();
		List<Map<String,Object>> keysToShow = new ArrayList<>();

	    //Normalizing lists for display
	    List<LanguageKey> lkeys = langAPI.getLanguageKeys(languageCode);
	    List<LanguageKey> skeys = langAPI.getLanguageKeys(languageCode, countryCode);

	    List<LanguageKey> filteredLKeys = lkeys;
	    List<LanguageKey> filteredSKeys = skeys;

	    if(UtilMethods.isSet(filter)) {
	    	filteredLKeys=filterList(lkeys, filter);
	    	filteredSKeys=filterList(skeys, filter);
	    }

	    TreeSet<String> allKeys=new TreeSet<>();
	    for (LanguageKey lk : filteredLKeys) {
            allKeys.add(lk.getKey());
        }
	    for (LanguageKey lk : filteredSKeys) {
            allKeys.add(lk.getKey());
        }

	    final int beginIdx=Math.max((page-1) * keysPerPage, 0);
	    final int endIdx=Math.min(page*keysPerPage, allKeys.size());

	    String[] keys=allKeys.toArray(new String[allKeys.size()]);

	    for(int i = beginIdx ; i < endIdx ; i++ ) {
	        String key=keys[i];

	        LanguageKey fake=new LanguageKey("", "", key, "");
	        int lpos=Collections.binarySearch(lkeys, fake);
	        int spos=Collections.binarySearch(skeys, fake);

	        String gValue="", sValue="";
	        if(lpos>=0) gValue=lkeys.get(lpos).getValue();
	        if(spos>=0) sValue=skeys.get(spos).getValue();

	        Map<String,Object> keyMap = new HashMap<>();
            keyMap.put("key", key);
            keyMap.put("generalValue", UtilMethods.webifyString(UtilMethods.escapeHTMLSpecialChars(gValue)));
            keyMap.put("specificValue", UtilMethods.webifyString(UtilMethods.escapeHTMLSpecialChars(sValue)));
            keyMap.put("idx", Integer.toString(keysToShow.size()));
            keysToShow.add(keyMap);
	    }

		//Adding the result counters as the first row of the results
		Map<String, Object> counters = new HashMap<>();
		result.add(counters);

		long total = allKeys.size();
		counters.put("total", total);

		counters.put("hasPrevious", page>1);

		if (page == 0)
			counters.put("hasNext", false);
		else
			counters.put("hasNext", endIdx < allKeys.size());

		int totalPages = 1;
		if (page != 0)
			totalPages = (int) Math.ceil((float) total / (float) keysPerPage);

		counters.put("begin", beginIdx+1);
		counters.put("end", endIdx);
		counters.put("totalPages", totalPages);

		result.addAll(keysToShow);
		return result;
	}

	/**
	 * Adds, updates and deletes language keys of one language, as sent by the language keys
	 * page. Only a user with access to the Languages or the Locales portlet can save keys, checked
	 * by {@link LanguageKeysAccess#requireLanguagesOrLocalesPortlet} before anything is read
	 * or saved.
	 *
	 * @param languageCode the language code, for example {@code en}
	 * @param countryCode  the country code, for example {@code US}
	 * @param keysToAdd    new keys, each as key, general value and specific value joined by the
	 *                     form separator
	 * @param keysToUpdate changed keys, in the same format as {@code keysToAdd}
	 * @param keysToDelete names of the keys to delete
	 * @return the confirmation message shown on the page
	 * @throws DotSecurityException if nobody is logged in, or the logged-in user has access to
	 *                              neither the Languages nor the Locales portlet
	 * @throws DotDataException     if the user's portlets cannot be loaded
	 * @throws PortalException      if the logged-in user cannot be resolved
	 * @throws SystemException      if the logged-in user cannot be resolved
	 */
	public String saveKeys(String languageCode,String countryCode,List<String> keysToAdd,List<String> keysToUpdate,List<String> keysToDelete)
			throws DotSecurityException, DotDataException, PortalException, SystemException {

		LanguageKeysAccess.requireLanguagesOrLocalesPortlet(DwrUtil.getLoggedInUser());

		Map<String, String> generalKeysToAdd = new HashMap<>();
		Map<String, String> specificKeysToAdd  = new HashMap<>();
		Set<String> deleteKeys = new HashSet<>();
		Language lang = langAPI.getLanguage(languageCode, countryCode);
		String delim = WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR;

		for(String str:keysToAdd){
			int firstDelimIndex = str.indexOf(delim);
			int secondDelimIndex = str.indexOf(delim,firstDelimIndex+1);
			String key = str.substring(0,firstDelimIndex);
			String generalValue = str.substring(firstDelimIndex+delim.length(),secondDelimIndex);
			String specificValue = str.substring(secondDelimIndex+delim.length());

			if(UtilMethods.isSet(generalValue)){
				generalKeysToAdd.put(key, generalValue);
			}
			if(UtilMethods.isSet(specificValue)){
				specificKeysToAdd.put(key, specificValue);
			}
		}

		for(String str:keysToUpdate){
			int firstDelimIndex = str.indexOf(delim);
			int secondDelimIndex = str.indexOf(delim,firstDelimIndex+1);
			String key = str.substring(0,firstDelimIndex);
			String generalValue = str.substring(firstDelimIndex+delim.length(),secondDelimIndex);
			String specificValue = str.substring(secondDelimIndex+delim.length());

			if(UtilMethods.isSet(generalValue)){
				generalKeysToAdd.put(key, generalValue);
			}
			if(UtilMethods.isSet(specificValue)){
				specificKeysToAdd.put(key, specificValue);
			}
		}
		for(String str:keysToDelete){
			deleteKeys.add(str);
		}

		try {
			langAPI.saveLanguageKeys(lang, generalKeysToAdd, specificKeysToAdd, deleteKeys);
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage());
		}

		try {
			return LanguageUtil.get(UtilMethods.getDefaultCompany().getCompanyId(), UtilMethods.getDefaultCompany().getLocale(), "message.languagemanager.save");
		} catch (LanguageException e) {
			return "message.languagemanager.save";
		}
	}
	
	public List<Map<String, String>> getLanguages() throws LanguageException, DotRuntimeException, PortalException, SystemException {
	    return getLanguages(false);
	}
	
	public List<Map<String, String>> getLanguagesWithAllOption() throws LanguageException, DotRuntimeException, PortalException, SystemException {
	    return getLanguages(true);
	}

	private List<Map<String, String>> getLanguages(boolean withAllOption) throws LanguageException, DotRuntimeException, PortalException, SystemException {
		List<Language> languages =  APILocator.getLanguageAPI().getLanguages();
		ArrayList<Map<String, String>> langList = new ArrayList<> ();
		UserWebAPI uWebAPI = WebAPILocator.getUserWebAPI();
		HttpServletRequest request = WebContextFactory.get().getHttpServletRequest();

		final String sLanguage = LanguageUtil.get(uWebAPI.getLoggedInUser(request), "Language");

		// Include ALL option
		if(withAllOption) {
		    Map<String, String> map = new HashMap<>();
		    map.put("title", sLanguage);
            map.put("languageCode", StringPool.BLANK);
            map.put("language", LanguageUtil.get(uWebAPI.getLoggedInUser(request), "all"));
            map.put("countryCode", StringPool.BLANK);
            map.put("country", StringPool.BLANK);
            map.put("id", StringPool.BLANK);
            langList.add(map);
		}
		
		for (Language language : languages) {
			Map<String, String> map = new HashMap<>();
			map.put("title", sLanguage);
			map.put("languageCode", language.getLanguageCode());
			map.put("language", language.getLanguage());
			map.put("countryCode", language.getCountryCode());
			map.put("country", language.getCountry());
			map.put("id", Long.toString(language.getId()));
			langList.add(map);
		}

		return langList;

	}
}
