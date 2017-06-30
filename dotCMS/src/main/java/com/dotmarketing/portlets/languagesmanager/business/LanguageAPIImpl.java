package com.dotmarketing.portlets.languagesmanager.business;

import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.repackage.org.apache.commons.lang.math.NumberUtils;
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
import com.liferay.util.StringPool;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * Implementation class for the {@link LanguageAPI}.
 * 
 * @author root
 * @version N/A
 * @since Mar 22, 2012
 *
 */
public class LanguageAPIImpl implements LanguageAPI {

	private HttpServletRequest request;
	private LanguageFactory factory;
	private LanguageVariableAPI languageVariableAPI;
	
	Context ctx;

	/**
	 * 
	 * @param obj
	 */
	public void init(Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest();
		ctx = context.getVelocityContext();
	}

	/**
	 * Creates a new instance of the {@link LanguageAPI}.
	 */
	public LanguageAPIImpl() {
		factory = FactoryLocator.getLanguageFactory();
	}

	@Override
	public void deleteLanguage(Language language) {
		factory.deleteLanguage(language);
	}

	@Override
	public Language getLanguage(String languageCode, String countryCode) {
		return factory.getLanguage(languageCode, countryCode);
	}

	@Override
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

	@Override
	public Language getLanguage(String id) {
		return factory.getLanguage(id);
	}

	@Override
	public Language createDefaultLanguage() {
		return factory.createDefaultLanguage();
	}

	@Override
	public Language getLanguage(long id) {
		return factory.getLanguage(id);
	}

	@Override
	public List<Language> getLanguages() {
		return factory.getLanguages();
	}

	@Override
	public void saveLanguage(Language language) {
		factory.saveLanguage(language);
	}

	@Override
	public String getLanguageCodeAndCountry(long id, String langId) {
		return factory.getLanguageCodeAndCountry(id, langId);
	}

	@Override
	public Language getDefaultLanguage() {
		return factory.getDefaultLanguage();
	}

	@Override
	public boolean hasLanguage(String id) {
		return factory.hasLanguage(id);
	}

	@Override
	public boolean hasLanguage(long id) {
		return factory.hasLanguage(id);
	}

	@Override
	public boolean hasLanguage(String languageCode, String countryCode) {
		return factory.hasLanguage(languageCode, countryCode);
	}

	@Override
	public List<LanguageKey> getLanguageKeys(String langCode) {
		List<LanguageKey> list = factory.getLanguageKeys(langCode);
		Collections.sort(list, new LanguageKeyComparator());
		return list;
	}

	@Override
	public List<LanguageKey> getLanguageKeys(String langCode, String countryCode) {
		List<LanguageKey> list = factory.getLanguageKeys(langCode, countryCode);
		Collections.sort(list, new LanguageKeyComparator());
		return list;
	}

	@Override
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

	@Override
	public void createLanguageFiles(Language lang) {
		factory.createLanguageFiles(lang);
	}

	@Override
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

	@Override
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
        // First, retrieve value from legacy Language Variables or the appropriate
        // Language.properties file
        String value = null;
        try {
            value = LanguageUtil.get( new Locale( lang.getLanguageCode(), lang.getCountryCode() ), key );
        } catch ( LanguageException e ) {
            Logger.error( this, e.getMessage(), e );
        }
        // If not found, look it up using the new Language Variable API
        if (null == value || StringPool.BLANK.equals(value.trim())) {
            value = getLanguageVariableAPI().get(key, lang.getId(), user, Boolean.TRUE);
        }
        return value;
    }

	@Override
	public boolean getBooleanKey(Language lang, String key) {
		return Boolean.parseBoolean(getStringKey(lang, key));
	}

	@Override
	public boolean getBooleanKey(Language lang, String key, boolean defaultVal) {
		if(getStringKey(lang, key) != null)
			return Boolean.parseBoolean(getStringKey(lang, key));
		return defaultVal;
	}

	@Override
	public float getFloatKey(Language lang, String key) {
		return Float.parseFloat(getStringKey(lang, key));
	}

	@Override
	public float getFloatKey(Language lang, String key, float defaultVal) {
		if(getStringKey(lang, key) != null)
			return Float.parseFloat(getStringKey(lang, key));
		return defaultVal;
	}

	@Override
	public int getIntKey(Language lang, String key) {
		return Integer.parseInt(getStringKey(lang, key));
	}

	@Override
	public int getIntKey(Language lang, String key, int defaultVal) {
		if(getStringKey(lang, key) != null)
			return Integer.parseInt(getStringKey(lang, key));
		return defaultVal;
	}

	@Override
	public void clearCache(){
		CacheLocator.getLanguageCache().clearCache();
	}

    @Override
    public Language getFallbackLanguage(String languageCode) {
        return this.factory.getFallbackLanguage(languageCode);
    }

    /**
     * Utility method used to get an instance of the {@link LanguageVariableAPI}. This is used to
     * avoid Stackoverflow exceptions when starting up the system.
     * 
     * @return An instance of the {@link LanguageVariableAPI}.
     */
    private LanguageVariableAPI getLanguageVariableAPI() {
        if (null == this.languageVariableAPI) {
            this.languageVariableAPI = APILocator.getLanguageVariableAPI();
        }
        return this.languageVariableAPI;
    }
    
}
