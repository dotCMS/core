package com.dotmarketing.portlets.languagesmanager.business;

import com.dotcms.languagevariable.business.LanguageVariable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Logger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * @author David
 */
public class LanguageCacheImpl extends LanguageCache {

	private static final String LANG_404_STR = "LANG__404";
	static final String ALL_LANGUAGES_KEY="ALL_LANGUAGES_KEY";
	static final String DEFAULT_LANGUAGE = "DEFAULT_LANGUAGE";

	private static final String  LANG_VARIABLES_CACHE = "LanguageVariablesCache";
	public static final String LANGUAGE_VARIABLES_FOR_LANGUAGE_WITH_ID = "Language Variables for language with id: ";
	public static final String HAS_BEEN_FETCHED_FROM_THE_DATABASE = " has been fetched from the database.";

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
		return "LanguageVariablesCacheGroup";
	}

	@Deprecated(since = "24.05", forRemoval = true)
	@Override
	public void setLanguageKeys(String langCode, String countryCode, List<LanguageKey> keys) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = getPrimaryGroup() + "_Keys_" + (countryCode != null?langCode + "_" + countryCode:langCode);
        cache.put(languageKey, keys, getPrimaryGroup());
	}

	@Deprecated(since = "24.05", forRemoval = true)
	@Override
	public void removeLanguageKeys(String langCode, String countryCode) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = getPrimaryGroup() + "_Keys_" + (countryCode != null?langCode + "_" + countryCode:langCode);
        cache.remove(languageKey, getPrimaryGroup());
	}

	@Deprecated(since = "24.05", forRemoval = true)
	@Override
	public List<LanguageKey> getLanguageKeys(String langCode, String countryCode) throws DotCacheException {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = getPrimaryGroup() + "_Keys_" + (countryCode != null?langCode + "_" + countryCode:langCode);
        return (List<LanguageKey>) cache.get(languageKey, getPrimaryGroup());
	}

	/**
	 * This method is used to clear the cache for a specific language
	 * @param languageId the language id to clear the cache for
	 */
	public void clearVarsByLang(final long languageId){
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		final Object perLangCache = cache.getNoThrow(LANG_VARIABLES_CACHE, group);
		if (perLangCache == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		final ConcurrentMap<String,Map<String,LanguageVariable>> langVarCache = (ConcurrentMap<String,Map<String,LanguageVariable>>) perLangCache;
		final String languageIdStr = String.valueOf(languageId);
		langVarCache.remove(languageIdStr);
		Logger.debug(this, LANGUAGE_VARIABLES_FOR_LANGUAGE_WITH_ID + languageId + " have been removed from cache.");
	}

	/**
	 * This method is used to clear the cache for all language variables
	 */
	public void clearVariables(){
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		cache.flushGroup(group);
	}

	/**
	 * if the there are languageVariables stored in cache for the given languageId this will return them
	 * otherwise it will fetch them from the database and store them in cache
	 * if an empty list is returned from the fetch callable, it will still be cached which is fine
	 * @return a list of LanguageVariables
	 */
	@Override
	public List<LanguageVariable> ifPresentGetOrElseFetch(final long languageId,
			final Callable<List<LanguageVariable>> fetchFunction) throws DotDataException {
		final String languageIdStr = String.valueOf(languageId);
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		final List<LanguageVariable> result;
		try {
			final Object perLangCache = cache.getNoThrow(LANG_VARIABLES_CACHE, group);
			if (perLangCache == null) {
				result = fetchFunction.call();
				Logger.debug(this, LANGUAGE_VARIABLES_FOR_LANGUAGE_WITH_ID + languageId + HAS_BEEN_FETCHED_FROM_THE_DATABASE);
				putVars(languageId, result);
			} else {
				@SuppressWarnings("unchecked") final ConcurrentMap<String, Map<String,LanguageVariable>> langVarCache = (ConcurrentMap<String, Map<String,LanguageVariable>>) perLangCache;
				final  Map<String,LanguageVariable> variables = langVarCache.get(languageIdStr);
				if (variables != null) {
					result = List.copyOf(variables.values());
				} else {
					result = fetchFunction.call();
					Logger.debug(this, LANGUAGE_VARIABLES_FOR_LANGUAGE_WITH_ID + languageId + HAS_BEEN_FETCHED_FROM_THE_DATABASE);
					putVars(languageId, result);
				}
			}
		} catch (Exception e) {
			throw new DotDataException(e);
		}
		// even thought the object LanguageVariable is immutable, the list is not therefore we can still have cache modification issues
		return List.copyOf(result);
	}


    /**
	 * This method is used to store a list of LanguageVariables in cache
	 * @param languageId the language id to store the list of LanguageVariables for
	 * @param vars the list of LanguageVariables to store
	 */
	@Override
	public void putVars(final long languageId, final List<LanguageVariable> vars){
		final String languageIdStr = String.valueOf(languageId);
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		Object perLangCache = cache.getNoThrow(LANG_VARIABLES_CACHE, group);
		if (perLangCache == null) {
			// A map of LanguageVariables is stored per language
			cache.put(LANG_VARIABLES_CACHE, new ConcurrentHashMap<String, Map<String,LanguageVariable>>(), group);
			perLangCache = cache.getNoThrow(LANG_VARIABLES_CACHE, group);
		}
		@SuppressWarnings("unchecked")
		final ConcurrentMap<String,Map<String,LanguageVariable>> langVarCache = (ConcurrentMap<String,Map<String,LanguageVariable>>) perLangCache;
		//Now we use the crafted (specific) key to store the list of LanguageVariables
		//So that if we want to invalidate only the list of LanguageVariables for a specific language it is possible
		langVarCache.put(languageIdStr, vars.stream().collect(Collectors.toMap(LanguageVariable::key, o -> o, mergeFunction())));
		cache.put(languageIdStr, langVarCache, group);
	}

	private static BinaryOperator<LanguageVariable> mergeFunction() {
		return (languageVariable, languageVariable2) -> languageVariable;
	}

	/**
	 * This method is used to retrieve a list of LanguageVariables from cache
	 * @param languageId the language id
	 * @return a list of LanguageVariables
	 */
	public List<LanguageVariable> getVars(final long languageId){
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		final String languageIdStr = String.valueOf(languageId);
		// A map of LanguageVariables is stored per language
		Object  perLangCache = cache.getNoThrow(LANG_VARIABLES_CACHE, group);
		if (perLangCache == null) {
			return List.of();
		}
		@SuppressWarnings("unchecked")
		final ConcurrentMap<String,Map<String,LanguageVariable>> langVarCache = (ConcurrentMap<String,Map<String,LanguageVariable>>) perLangCache;
		//Now we use the crafted (specific) key to access the list of LanguageVariables
		final Map<String,LanguageVariable> variables = langVarCache.get(languageIdStr);
		if (variables == null) {
			return List.of();
		}
	    return 	List.copyOf(variables.values());
	}

	/**
	 * This method is used to retrieve a LanguageVariable from cache
	 * @param languageId the language id
	 * @param key the key of the LanguageVariable
	 * @return a LanguageVariable
	 */
	public Optional<LanguageVariable> getVar(final long languageId, final String key){
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		final String languageIdStr = String.valueOf(languageId);
		final Object perLangCache = cache.getNoThrow(LANG_VARIABLES_CACHE, group);
		if (perLangCache == null) {
			return Optional.empty();
		}
        @SuppressWarnings("unchecked")
		final ConcurrentMap<String,Map<String,LanguageVariable>> langVarCache = (ConcurrentMap<String,Map<String,LanguageVariable>>) perLangCache;
		final Map<String, LanguageVariable> variableMap = langVarCache.get(languageIdStr);
		if (variableMap == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(variableMap.get(key));
	}


	/**
	 * This method is used to retrieve a LanguageVariable from cache
	 * @param languageId the language id
	 * @param key  the key
	 * @param fetchFunction the fetch function
	 * @return a LanguageVariable
	 * @throws DotDataException
	 */

	public Optional<LanguageVariable> ifPresentGetOrElseFetch(final long languageId, final String key,
			final Callable<List<LanguageVariable>> fetchFunction) throws DotDataException {
		final String group = getSecondaryGroup();
		final DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		try {
			final Object perLangCache = cache.getNoThrow(LANG_VARIABLES_CACHE, group);
			@SuppressWarnings("unchecked")
			final ConcurrentMap<String, Map<String, LanguageVariable>> langVarCache = (ConcurrentMap<String, Map<String, LanguageVariable>>) perLangCache;
			if (langVarCache == null) {
				List<LanguageVariable> result = fetchFunction.call();
				Logger.debug(this, LANGUAGE_VARIABLES_FOR_LANGUAGE_WITH_ID + languageId + HAS_BEEN_FETCHED_FROM_THE_DATABASE);
				putVars(languageId, result);
			} else {
				final String languageIdStr = String.valueOf(languageId);
				final Map<String, LanguageVariable> variableMap = langVarCache.get(languageIdStr);
				if (variableMap == null || variableMap.get(key) == null) {
					List<LanguageVariable> result = fetchFunction.call();
					Logger.debug(this, LANGUAGE_VARIABLES_FOR_LANGUAGE_WITH_ID + languageId + HAS_BEEN_FETCHED_FROM_THE_DATABASE);
					putVars(languageId, result);
				}
			}
			return getVar(languageId, key);
		} catch (Exception e) {
			throw new DotDataException(e);
		}
	}


}
