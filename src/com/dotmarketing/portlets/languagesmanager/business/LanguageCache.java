package com.dotmarketing.portlets.languagesmanager.business;

import java.util.List;

import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;

/**
 * @author David
 */
public abstract class LanguageCache implements Cachable{

    protected abstract void addLanguage(Language l);
    
    protected abstract Language getLanguageById(long id);

    protected abstract Language getLanguageById(String id);

    protected abstract Language getLanguageByCode(String languageCode, String countryCode);

    protected abstract boolean hasLanguage (String id);
    
    protected abstract boolean hasLanguage (long id);
    
    protected abstract boolean hasLanguage (String languageCode, String countryCode);
    
    protected abstract void removeLanguage(Language l);

    public abstract void clearCache();
    
	public abstract String[] getGroups();
    
    public abstract String getPrimaryGroup();
    
    protected abstract List<LanguageKey> getLanguageKeys(String langCode, String countryCode) throws DotCacheException;

    protected abstract void removeLanguageKeys(String langCode, String countryCode);

    protected abstract void setLanguageKeys(String langCode, String countryCode, List<LanguageKey> keys);
}
