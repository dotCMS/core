package com.dotmarketing.portlets.languagesmanager.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import edu.emory.mathcs.backport.java.util.Collections;

public class LanguageAPIImpl implements LanguageAPI {
	
	private HttpServletRequest request;
	Context ctx;

	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();
	}

	private LanguageFactory factory;
	
	public LanguageAPIImpl() {
		factory = FactoryLocator.getLanguageFactory();
	}
	
	public void deleteLanguage(Language language) {
		factory.deleteLanguage(language);
	}

	public Language getLanguage(String languageCode, String countryCode) {
		return factory.getLanguage(languageCode, countryCode);
	}

	public Language getLanguage(String id) {
		return factory.getLanguage(id);

	}

	public Language createDefaultLanguage() {
		return factory.createDefaultLanguage();

	}

	public Language getLanguage(long id) {
		return factory.getLanguage(id);

	}

	public List<Language> getLanguages() {
		return factory.getLanguages();

	}

	public void saveLanguage(Language o) {
		factory.saveLanguage(o);
	}

	public String getLanguageCodeAndCountry(long id, String langId) {
		return factory.getLanguageCodeAndCountry(id, langId);

	}

	public Language getDefaultLanguage() {
		return factory.getDefaultLanguage();

	}

	public boolean hasLanguage(String id) {
		return factory.hasLanguage(id);
	}

	public boolean hasLanguage(long id) {
		return factory.hasLanguage(id);
	}

	public boolean hasLanguage(String languageCode, String countryCode) {
		return factory.hasLanguage(languageCode, countryCode);
	}

	public List<LanguageKey> getLanguageKeys(String langCode) {
		List<LanguageKey> list = factory.getLanguageKeys(langCode);
		Collections.sort(list, new LanguageKeyComparator());
		return list;
	}

	public List<LanguageKey> getLanguageKeys(String langCode, String countryCode) {
		List<LanguageKey> list = factory.getLanguageKeys(langCode, countryCode);
		Collections.sort(list, new LanguageKeyComparator());
		return list;
	}
	
	public List<LanguageKey> getLanguageKeys(Language lang) {
		String langCode = lang.getLanguageCode();
		String countryCode = lang.getCountryCode();
		List<LanguageKey> list = new ArrayList<LanguageKey>();
		list.addAll(factory.getLanguageKeys(langCode));
		Collections.sort(list, new LanguageKeyComparator());
		
		List<LanguageKey> keys = factory.getLanguageKeys(langCode, countryCode);
		for(LanguageKey key : keys) {
			int index = -1;
			if((index = Collections.binarySearch(list, key, new LanguageKeyComparator())) >= 0) {
				list.remove(index);
			}
			list.add(key);
		}
		
		Collections.sort(list, new LanguageKeyComparator());
		return list;
	}



	public void createLanguageFiles(Language lang) {
		factory.createLanguageFiles(lang);
	}

	public void saveLanguageKeys(Language lang, Map<String, String> generalKeys, Map<String, String> specificKeys, Set<String> toDeleteKeys) throws DotDataException {
		factory.saveLanguageKeys(lang, generalKeys, specificKeys, toDeleteKeys);
		
	}
	
	public void addLanguageKeys(Language lang, Map<String, String> generalKeys, Map<String, String> specificKeys) throws DotDataException, LanguageException {
		
		List<LanguageKey> existingGeneralKeys = getLanguageKeys(lang.getLanguageCode());
		List<LanguageKey> existingSpecificKeys = getLanguageKeys(lang.getLanguageCode(),lang.getCountryCode());
		
		for(LanguageKey key:existingGeneralKeys){
			if(generalKeys.containsKey(key.getKey()))				
				throw new DotDataException(LanguageUtil.get(UtilMethods.getDefaultCompany().getCompanyId(),UtilMethods.getDefaultCompany().getLocale(), "message.languagemanager.key.already.registered"));			
		}
		for(LanguageKey key:existingSpecificKeys){
			if(specificKeys.containsKey(key.getKey()))
				throw new DotDataException(LanguageUtil.get(UtilMethods.getDefaultCompany().getCompanyId(),UtilMethods.getDefaultCompany().getLocale(), "message.languagemanager.key.already.registered"));
		}
		
		for(LanguageKey key:existingGeneralKeys){
			generalKeys.put(key.getKey(), key.getValue());			
		}
		for(LanguageKey key:existingSpecificKeys){
			specificKeys.put(key.getKey(), key.getValue());			
		}
		
		factory.saveLanguageKeys(lang, generalKeys, specificKeys, null);
		
	}
	
	public void updateLanguageKeys(Language lang, Map<String, String> generalKeys, Map<String, String> specificKeys) throws DotDataException, LanguageException {
		
		List<LanguageKey> existingGeneralKeys = getLanguageKeys(lang.getLanguageCode());
		List<LanguageKey> existingSpecificKeys = getLanguageKeys(lang.getLanguageCode(),lang.getCountryCode());
		
		for(LanguageKey key:existingGeneralKeys){
			if(generalKeys.containsKey(key.getKey())){
				key.setValue(generalKeys.get(key.getKey()));
				generalKeys.remove(key.getKey());
			}
		}
		for(LanguageKey key:existingSpecificKeys){
			if(specificKeys.containsKey(key.getKey())){
				key.setValue(specificKeys.get(key.getKey()));
				specificKeys.remove(key.getKey());
			}
		}		
		
		for(LanguageKey key:existingGeneralKeys){
			generalKeys.put(key.getKey(), key.getValue());			
		}
		for(LanguageKey key:existingSpecificKeys){
			specificKeys.put(key.getKey(), key.getValue());			
		}
		
		factory.saveLanguageKeys(lang, generalKeys, specificKeys, null);		
	}
	
	public void deleteLanguageKeys(Language lang,Set<String> toDeleteKeys) throws DotDataException, LanguageException {
		
		List<LanguageKey> existingGeneralKeys = getLanguageKeys(lang.getLanguageCode());
		List<LanguageKey> existingSpecificKeys = getLanguageKeys(lang.getLanguageCode(),lang.getCountryCode());
		Map<String, String> generalKeys = new HashMap<String, String>();
		Map<String, String> specificKeys = new HashMap<String, String>();
		
		for(LanguageKey key:existingGeneralKeys){
			generalKeys.put(key.getKey(), key.getValue());
		}
		for(LanguageKey key:existingSpecificKeys){
			specificKeys.put(key.getKey(), key.getValue());
		}
		
		factory.saveLanguageKeys(lang, generalKeys, specificKeys, toDeleteKeys);		
	}

	
		public String getStringKey (Language lang, String key){
			User user1=null;
			try {
				user1 = com.liferay.portal.util.PortalUtil.getUser(this.request);
				
			} catch (Exception e) {
				Logger.debug(this, e.getMessage(), e);
			}
			
			if(user1==null)
			{ 
				try {
					user1=APILocator.getUserAPI().getSystemUser();
				} catch (DotDataException e) {
					Logger.debug(this, e.getMessage(), e);
				}
			}
			String value=null;
			List<LanguageKey> keys = getLanguageKeys(lang.getLanguageCode(), lang.getCountryCode());
			for(LanguageKey keyEntry : keys) {
				if(keyEntry.getKey().equals(key))
					value= keyEntry.getValue();
				}
				keys = getLanguageKeys(lang.getLanguageCode());
				for(LanguageKey keyEntry : keys) {
				if(keyEntry.getKey().equals(key))
					value= keyEntry.getValue();
			}
			if(value==null)
			{
			 
				try {
					//if(user1 != null)
						value=LanguageUtil.get(user1, key);
				} catch (LanguageException e) {
					Logger.error(this, e.getMessage(), e);
				} 
			
			}
			return value;
		}

	public boolean getBooleanKey(Language lang, String key) {
		return Boolean.parseBoolean(getStringKey(lang, key));
	}

	public boolean getBooleanKey(Language lang, String key, boolean defaultVal) {
		if(getStringKey(lang, key) != null)
			return Boolean.parseBoolean(getStringKey(lang, key));
		return defaultVal;
	}

	public float getFloatKey(Language lang, String key) {
		return Float.parseFloat(getStringKey(lang, key));
	}

	public float getFloatKey(Language lang, String key, float defaultVal) {
		if(getStringKey(lang, key) != null)
			return Float.parseFloat(getStringKey(lang, key));
		return defaultVal;
	}

	public int getIntKey(Language lang, String key) {
		return Integer.parseInt(getStringKey(lang, key));
	}

	public int getIntKey(Language lang, String key, int defaultVal) {
		if(getStringKey(lang, key) != null)
			return Integer.parseInt(getStringKey(lang, key));
		return defaultVal;
	}

	public void clearCache(){
		CacheLocator.getLanguageCache().clearCache();
	}
}
