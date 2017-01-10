package com.dotmarketing.portlets.languagesmanager.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Collections;
import com.dotcms.repackage.org.apache.commons.lang.math.NumberUtils;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;

import javax.servlet.http.HttpServletRequest;

import java.util.*;

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

    public boolean isAssetTypeLanguage(String id) {
        if (!NumberUtils.isDigits(id)) {
            return false;
        }

        try {
            Language language = factory.getLanguage(Long.parseLong(id));
            if (language != null && UtilMethods.isSet(language.getLanguage())) {
                return true;
            }
        } catch (Exception e) {}

        return false;
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
		factory.saveLanguageKeys(lang, generalKeys, specificKeys, toDeleteKeys);

	}

    /**
     * Returns a internationalized value for a given key and language
     *
     * @param lang
     * @param key
     * @return
     */
    public String getStringKey ( Language lang, String key ) {

        User user = null;
        try {
            user = com.liferay.portal.util.PortalUtil.getUser( this.request );
        } catch ( Exception e ) {
            Logger.debug( this, e.getMessage(), e );
        }

        if ( user == null ) {
            try {
                user = APILocator.getUserAPI().getSystemUser();
            } catch ( DotDataException e ) {
                Logger.debug( this, e.getMessage(), e );
            }
        }

        String value = null;
        try {
            value = LanguageUtil.get( new Locale( lang.getLanguageCode(), lang.getCountryCode() ), key );
        } catch ( LanguageException e ) {
            Logger.error( this, e.getMessage(), e );
        }

        //If we didn't find a value for the given language, lets try with the default one
        if ( value == null ) {
            try {
                value = LanguageUtil.get( user, key );//Searching this key in the default language
            } catch ( LanguageException e ) {
                Logger.error( this, e.getMessage(), e );
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
