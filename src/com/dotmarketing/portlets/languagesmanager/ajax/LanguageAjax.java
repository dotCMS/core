package com.dotmarketing.portlets.languagesmanager.ajax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.struts.Globals;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.util.servlet.SessionMessages;

import edu.emory.mathcs.backport.java.util.Collections;

public class LanguageAjax {

	public LanguageAPI langAPI = APILocator.getLanguageAPI();
	
	public List<LanguageKey> getLanguageKeys(String languageCode) {
		return getLanguageKeys(languageCode, null);
	}

	public List<LanguageKey> getLanguageKeys(String languageCode, String countryCode) {
		return langAPI.getLanguageKeys(languageCode, countryCode);
	}
	
	public List<Map<String,Object>> getPaginatedLanguageKeys(String languageCode,int page) {
		return getPaginatedLanguageKeys(languageCode, null,page);
	}

	public List<Map<String,Object>> getPaginatedLanguageKeys(String languageCode, String countryCode,int page) {
		
		int keysPerPage = 100;
		int keyIndex = 1;
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		List<Map<String,Object>> keysToShow = new ArrayList<Map<String,Object>>();
			
	    //Normalizing lists for display
	    List<LanguageKey> generalKeysList = new LinkedList<LanguageKey>(langAPI.getLanguageKeys(languageCode));
	    Collections.sort(generalKeysList);
	    List<LanguageKey> specificKeyList = new LinkedList<LanguageKey>(langAPI.getLanguageKeys(languageCode, countryCode));
	    Collections.sort(specificKeyList);
	    
	    for(LanguageKey key: generalKeysList) {
	    	int pos = Collections.binarySearch(specificKeyList, key);
	    	if(pos < 0) {
	    		LanguageKey newKey = new LanguageKey(key.getLanguageCode(), key.getCountryCode(), key.getKey(), "");
	    		specificKeyList.add(-(pos + 1), newKey);
	    	}
	    }

	    for(LanguageKey key: specificKeyList) {
	    	int pos = Collections.binarySearch(generalKeysList, key);
	    	if(pos < 0) {
	    		LanguageKey newKey = new LanguageKey(key.getLanguageCode(), key.getCountryCode(), key.getKey(), "");
	    		generalKeysList.add(-(pos + 1), newKey);
	    	}
	    }

	    String alternate = "alternate_1";
		int idx = 0; 
		for(keyIndex = ((page-1) * keysPerPage); (keyIndex <= ((page * keysPerPage)+1) && (keyIndex < generalKeysList.size())); keyIndex++) {
			LanguageKey generalKey = generalKeysList.get(keyIndex);
			int pos = Collections.binarySearch(specificKeyList, generalKey);
			LanguageKey specificKey;
			if(pos >= 0)
				specificKey = specificKeyList.get(pos);
			else
				specificKey = new LanguageKey(generalKey.getLanguageCode(), generalKey.getCountryCode(), generalKey.getKey(), "");
			
			Map<String,Object> keyMap = new HashMap<String, Object>();				
			keyMap.put("key", generalKey.getKey());				
			keyMap.put("generalValue", UtilMethods.webifyString(UtilMethods.escapeHTMLSpecialChars(generalKey.getValue())));
			keyMap.put("specificValue", UtilMethods.webifyString(UtilMethods.escapeHTMLSpecialChars(specificKey.getValue())));
			keyMap.put("idx", ""+idx);
			keyMap.put("alternate", alternate);
			keysToShow.add(keyMap);
			alternate = alternate.equals("alternate_1")?"alternate_2":"alternate_1";
			idx++;
		}
		
		//Adding the result counters as the first row of the results
		Map<String, Object> counters = new HashMap<String, Object>();
		result.add(counters);
		
		long total = generalKeysList.size();
		counters.put("total", total);
		if (page == 0)
			counters.put("hasPrevious", false);
		else
			counters.put("hasPrevious", page != 1);

		if (page == 0)
			counters.put("hasNext", false);
		else
			counters.put("hasNext", keysPerPage < keysToShow.size());

		long end = total;
		if (page != 0)
			end = page * keysPerPage;

		end = (end < total ? end : total);

		int begin = 1;
		if (page != 0)
			begin = (page == 0 ? 0 : (page - 1) * keysPerPage);

		begin = (end != 0 ? begin + 1: begin);

		int totalPages = 1;
		if (page != 0)
			totalPages = (int) Math.ceil((float) total / (float) keysPerPage);

		counters.put("begin", begin);
		counters.put("end", end);
		counters.put("totalPages", totalPages);
		
		result.addAll(keysToShow);
		return result;
	}
	
	public String saveKeys(String languageCode,String countryCode,List<String> keysToAdd,List<String> keysToUpdate,List<String> keysToDelete){
		
		Map<String, String> generalKeysToAdd = new HashMap<String, String>();
		Map<String, String> specificKeysToAdd  = new HashMap<String, String>();
		Map<String, String> generalKeysToUpdate = new HashMap<String, String>();
		Map<String, String> specificKeysToUpdate  = new HashMap<String, String>();
		Set<String> deleteKeys = new HashSet<String>();
		Language lang = langAPI.getLanguage(languageCode, countryCode);
		String delim = WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR;
		
		for(String str:keysToAdd){
			int firstDelimIndex = str.indexOf(delim);
			int secondDelimIndex = str.indexOf(delim,firstDelimIndex+1);
			String key = str.substring(0,firstDelimIndex);
			String generalValue = str.substring(firstDelimIndex+delim.length(),secondDelimIndex);
			String specificValue = str.substring(secondDelimIndex+delim.length());
			
			generalKeysToAdd.put(key, generalValue);
			specificKeysToAdd.put(key, specificValue);			
		}
		
		for(String str:keysToUpdate){
			int firstDelimIndex = str.indexOf(delim);
			int secondDelimIndex = str.indexOf(delim,firstDelimIndex+1);
			String key = str.substring(0,firstDelimIndex);
			String generalValue = str.substring(firstDelimIndex+delim.length(),secondDelimIndex);
			String specificValue = str.substring(secondDelimIndex+delim.length());
			
			generalKeysToUpdate.put(key, generalValue);
			specificKeysToUpdate.put(key, specificValue);			
		}		
		for(String str:keysToDelete){		
			deleteKeys.add(str);
		}
		
		try {
			langAPI.addLanguageKeys(lang, generalKeysToAdd, specificKeysToAdd);
			langAPI.updateLanguageKeys(lang, generalKeysToUpdate, specificKeysToUpdate);
			langAPI.deleteLanguageKeys(lang, deleteKeys);
		} catch (LanguageException e) {
			Logger.error(this, e.getMessage());
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage());
		}
		
		try {
			return LanguageUtil.get(UtilMethods.getDefaultCompany().getCompanyId(), UtilMethods.getDefaultCompany().getLocale(), "message.languagemanager.save");
		} catch (LanguageException e) {			
			return "message.languagemanager.save";
		}
	}	
}
