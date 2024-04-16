package com.dotmarketing.portlets.languagesmanager.business;

import com.dotmarketing.portlets.languagesmanager.model.LanguageVariable;
import com.liferay.util.StringPool;
import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author David
 */
public class LanguageCacheImpl extends LanguageCache {

	private static final String LANG_404_STR = "LANG__404";
	static final String ALL_LANGUAGES_KEY="ALL_LANGUAGES_KEY";
	static final String DEFAULT_LANGUAGE = "DEFAULT_LANGUAGE";
	public static Language LANG_404 = new Language(-1,
			LANG_404_STR, LANG_404_STR, LANG_404_STR,
			LANG_404_STR);

	@Override
    public List<Language> getLanguages()  {
	
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	
    	
		try {
			return (List<Language>) cache.get( ALL_LANGUAGES_KEY, getPrimaryGroup());
		} catch (DotCacheException e) {
			return null;
		}
    	
    	
    }
    public void putLanguages(List<Language> languages) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	cache.put(ALL_LANGUAGES_KEY, languages, getPrimaryGroup());
    	
    }

	public void clearLanguages() {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		cache.remove(ALL_LANGUAGES_KEY, getPrimaryGroup());
	}

	public void clearDefaultLanguage() {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		cache.remove(DEFAULT_LANGUAGE, getPrimaryGroup());
	}

	public Language getDefaultLanguage(){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		Language defaultLang = LANG_404;
		try {
			defaultLang = (Language)cache.get(DEFAULT_LANGUAGE, getPrimaryGroup());
			if(null == defaultLang){
			   cache.put(DEFAULT_LANGUAGE, LANG_404, getPrimaryGroup());
			   defaultLang = LANG_404;
			}
		} catch (DotCacheException e) {
			Logger.debug(LanguageCacheImpl.class,"Default Language not found in Cache.", e);
		}
		return defaultLang;
	}

	public void setDefaultLanguage(Language defaultLanguage){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		cache.put(DEFAULT_LANGUAGE, defaultLanguage, getPrimaryGroup());
	}

    public void addLanguage(Language l) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		long id = l.getId();
        String idSt = String.valueOf(l.getId());
        String languageKey = l.getLanguageCode() + "-" + l.getCountryCode();
		cache.put(getPrimaryGroup() + id, l, getPrimaryGroup());
        cache.put(getPrimaryGroup() + idSt, l, getPrimaryGroup());
        cache.put(getPrimaryGroup() + languageKey, l, getPrimaryGroup());
		cache.remove(ALL_LANGUAGES_KEY, getPrimaryGroup());

	}

	@Override
	public void add404Language(final String languageCode, String countryCode) {
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		String languageKey = languageCode + "-" + countryCode;
		cache.put(getPrimaryGroup() + languageKey, LANG_404, getPrimaryGroup());
	}
    
    public Language getLanguageById(long id){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	Language f = null;
    	try{
    		f = (Language) cache.get(getPrimaryGroup() + id,getPrimaryGroup());
    	}catch (DotCacheException e) {
			Logger.debug(LanguageCacheImpl.class,"Cache Entry not found", e);
    	}
        return f;
	}

    public Language getLanguageById(String id) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
    	Language f = null;
    	try{
    		f = (Language) cache.get(getPrimaryGroup() + id,getPrimaryGroup());
    	}catch (DotCacheException e) {
			Logger.debug(LanguageCacheImpl.class,"Cache Entry not found", e);
    	}
        return f;
    }

    public Language getLanguageByCode(String languageCode, String countryCode) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = languageCode + "-" + countryCode;
        languageKey = languageKey.toLowerCase();
        Language l = null;
        try{
        	l = (Language) cache.get(getPrimaryGroup() + languageKey,getPrimaryGroup());
        }catch (DotCacheException e) {
			Logger.debug(LanguageCacheImpl.class,"Cache Entry not found", e);
    	}
        
        return l;
    }

    public boolean hasLanguage (String id) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        Language l = null;
    	try{
    		l = (Language) cache.get(getPrimaryGroup() + id,getPrimaryGroup());
    	}catch (DotCacheException e) {
			Logger.debug(LanguageCacheImpl.class,"Cache Entry not found", e);
    	}
        return l != null;
    }
    
    public boolean hasLanguage (long id) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        Language l = null;
    	try{
    		l = (Language) cache.get(getPrimaryGroup() + id,getPrimaryGroup());
    	}catch (DotCacheException e) {
			Logger.debug(LanguageCacheImpl.class,"Cache Entry not found", e);
    	}
        return l != null;
    }

    public boolean hasLanguage (String languageCode, String countryCode) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = languageCode + "-" + countryCode;
        Language l = null;
        try{
        	l = (Language) cache.get(getPrimaryGroup() + languageKey,getPrimaryGroup());
        }catch (DotCacheException e) {
			Logger.debug(LanguageCacheImpl.class,"Cache Entry not found", e);
    	}
        return l != null;
    }
	
	public void removeLanguage(Language language) {

		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

		// Cleaning up the cached version of the language, it could be different as the one passed
		// as parameter if, for example, the ISO code was changed
		final var cachedLanguage = getLanguageById(language.getId());
		if (cachedLanguage != null) {
			removeFromCache(cachedLanguage);
		}

		// Cleaning up the language
		removeFromCache(language);

		cache.remove(ALL_LANGUAGES_KEY, getPrimaryGroup());
	}

	/**
	 * Removes the language from the cache
	 *
	 * @param language the language to remove
	 */
	private void removeFromCache(Language language) {

		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

		long id = language.getId();
		String idSt = String.valueOf(language.getId());
		String languageKey = language.getLanguageCode() + "-" + language.getCountryCode();
		languageKey = languageKey.toLowerCase();

		cache.remove(getPrimaryGroup() + id, getPrimaryGroup());
		cache.remove(getPrimaryGroup() + idSt, getPrimaryGroup());
		cache.remove(getPrimaryGroup() + languageKey, getPrimaryGroup());
	}

    public void clearCache(){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
	    //clear the cache
		for (String group : getGroups()) {
			cache.flushGroup(group);
		}

	}
	public String[] getGroups() {
    	return new String[]{getPrimaryGroup(), getSecondaryGroup()};
    }
    
    public String getPrimaryGroup() {
    	return "LanguageCacheImpl";
    }

	public String getSecondaryGroup() {
		return "LanguageVariablesCacheImpl";
	}

	@Deprecated(since = "24.04", forRemoval = true)
	@Override
	public void setLanguageKeys(String langCode, String countryCode, List<LanguageKey> keys) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = getPrimaryGroup() + "_Keys_" + (countryCode != null?langCode + "_" + countryCode:langCode);
        cache.put(languageKey, keys, getPrimaryGroup());
	}

	@Deprecated(since = "24.04", forRemoval = true)
	@Override
	public void removeLanguageKeys(String langCode, String countryCode) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = getPrimaryGroup() + "_Keys_" + (countryCode != null?langCode + "_" + countryCode:langCode);
        cache.remove(languageKey, getPrimaryGroup());
	}

	@Deprecated(since = "24.04", forRemoval = true)
	@Override
	public List<LanguageKey> getLanguageKeys(String langCode, String countryCode) throws DotCacheException {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = getPrimaryGroup() + "_Keys_" + (countryCode != null?langCode + "_" + countryCode:langCode);
        return (List<LanguageKey>) cache.get(languageKey, getPrimaryGroup());
	}

	/**
	 * This method is used to clear the cache for a specific language
	 * @param languageId
	 */
	public void clearVarsByLang(final long languageId){
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		final String languageIdStr = String.valueOf(languageId);
		cache.remove(languageIdStr, group);
	}

	public void clearVariables(){
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		cache.flushGroup(group);
	}

	String craftKey(final long languageId, final int limit, final int offset, final String orderBy){
		final int offsetVal = offset >= 0 ? offset : -1;
		final int limitVal = limit > 0 ? limit : -1;
		return String.format("LanguageVariable::lang:%d::limit:%s::offset%s::orderBy:%s", languageId, limitVal, offsetVal, orderBy);
	}

	@Override
	public void putVars(final long languageId, final List<LanguageVariable> vars, final int limit, final int offset,
			String orderBy){
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		final String languageIdStr = String.valueOf(languageId);
		final String key = craftKey(languageId, limit, offset, orderBy);

		Object  perLangCache = cache.getNoThrow(languageIdStr, group);
		if (perLangCache == null) {
			// A map of LanguageVariables is stored per language
			cache.put(languageIdStr, new ConcurrentHashMap<String,List<LanguageVariable>>(), group);
			perLangCache = cache.getNoThrow(languageIdStr, group);
		}
		@SuppressWarnings("unchecked")
		final ConcurrentMap<String,List<LanguageVariable>> langVarCache = (ConcurrentMap<String,List<LanguageVariable>>) perLangCache;
		//Now we use the crafted (specific) key to store the list of LanguageVariables
		//So that if we want to invalidate only the list of LanguageVariables for a specific language it is possible
		langVarCache.put(key, vars);
		cache.put(key, langVarCache, group);
	}

	public List<LanguageVariable> getVars(final long languageId, final int limit, final int offset,
		final String orderBy){
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		final String languageIdStr = String.valueOf(languageId);

		final String key = craftKey(languageId, limit, offset, orderBy);
		// A map of LanguageVariables is stored per language
		Object  perLangCache = cache.getNoThrow(languageIdStr, group);
		if (perLangCache == null) {
			return List.of();
		}
		@SuppressWarnings("unchecked")
		final ConcurrentMap<String,List<LanguageVariable>> langVarCache = (ConcurrentMap<String,List<LanguageVariable>>) perLangCache;
		//Now we use the crafted (specific) key to access the list of LanguageVariables
		return langVarCache.get(key);
	}

}
