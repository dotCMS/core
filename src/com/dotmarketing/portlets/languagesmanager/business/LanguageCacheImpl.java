package com.dotmarketing.portlets.languagesmanager.business;

import java.util.List;

import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Logger;

/**
 * @author David
 */
public class LanguageCacheImpl extends LanguageCache {

    public void addLanguage(Language l) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
		long id = l.getId();
        String idSt = String.valueOf(l.getId());
        String languageKey = l.getLanguageCode() + "-" + l.getCountryCode();
		cache.put(getPrimaryGroup() + id, l, getPrimaryGroup());
        cache.put(getPrimaryGroup() + idSt, l, getPrimaryGroup());
        cache.put(getPrimaryGroup() + languageKey, l, getPrimaryGroup());
        
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
    
    public void removeLanguage(Language l){
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        long id = l.getId();
        String idSt = String.valueOf(l.getId());
        String languageKey = l.getLanguageCode() + "-" + l.getCountryCode();
        cache.remove(getPrimaryGroup() + id,getPrimaryGroup());
        cache.remove(getPrimaryGroup() + idSt,getPrimaryGroup());
        cache.remove(getPrimaryGroup() + languageKey,getPrimaryGroup());
    }

    public void clearCache(){
		DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
	    //clear the cache
	    cache.flushGroup(getPrimaryGroup());
	}
	public String[] getGroups() {
    	String[] groups = {getPrimaryGroup()};
    	return groups;
    }
    
    public String getPrimaryGroup() {
    	return "LanguageCacheImpl";
    }

	@Override
	public void setLanguageKeys(String langCode, String countryCode, List<LanguageKey> keys) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = getPrimaryGroup() + "_Keys_" + (countryCode != null?langCode + "_" + countryCode:langCode);
        cache.put(languageKey, keys, getPrimaryGroup());
	}    

	@Override
	public void removeLanguageKeys(String langCode, String countryCode) {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = getPrimaryGroup() + "_Keys_" + (countryCode != null?langCode + "_" + countryCode:langCode);
        cache.remove(languageKey, getPrimaryGroup());
	}    

	@Override
	public List<LanguageKey> getLanguageKeys(String langCode, String countryCode) throws DotCacheException {
    	DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();
        String languageKey = getPrimaryGroup() + "_Keys_" + (countryCode != null?langCode + "_" + countryCode:langCode);
        return (List<LanguageKey>) cache.get(languageKey, getPrimaryGroup());
	}    
}
