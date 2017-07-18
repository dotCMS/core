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
import org.apache.velocity.tools.view.context.ViewContext;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.dotmarketing.db.LocalTransaction.wrap;
import static com.dotmarketing.db.LocalTransaction.wrapReturn;

/**
 * Implementation class for the {@link LanguageAPI}.
 * 
 * @author root
 * @version N/A
 * @since Mar 22, 2012
 *
 */
public class LanguageAPIImpl implements LanguageAPI {


    private final static LanguageKeyComparator LANGUAGE_KEY_COMPARATOR = new LanguageKeyComparator();

	private HttpServletRequest request; // todo: this should be decouple from the api
	private LanguageFactory factory;
	private LanguageVariableAPI languageVariableAPI;
	
	/**
	 * Inits the service with the user {@link ViewContext}
	 * @param obj
	 */
	public void init(final Object obj) {
		ViewContext context = (ViewContext) obj;
		this.request = context.getRequest(); // todo: this is just getting it for the user, so instead of getting the request should get the user.
	}

	/**
	 * Creates a new instance of the {@link LanguageAPI}.
	 */
	public LanguageAPIImpl() {
		factory = FactoryLocator.getLanguageFactory();
	}

	@Override
	public void deleteLanguage(final Language language) {

        try {

            wrap(() -> factory.deleteLanguage(language));
            Logger.debug(this, "deleteLanguage");
        } catch (DotDataException e) {

            if ( Logger.isErrorEnabled(LanguageAPIImpl.class) ) {
                Logger.error(LanguageAPIImpl.class, e.getMessage(), e);
            }
        }

	}

    @Override
    public void deleteFallbackLanguage(final Language fallbackLanguage) {

	    int rowsAffected = 0;

		try {

            rowsAffected = wrapReturn(() -> this.factory.deleteLanguageById(fallbackLanguage));
            Logger.debug(this, "deleteFallbackLanguage, rowsAffected: " + rowsAffected);
        } catch (DotDataException e) {

			if ( Logger.isErrorEnabled(LanguageAPIImpl.class) ) {
				Logger.error(LanguageAPIImpl.class, e.getMessage(), e);
			}
		}
	} // deleteFallbackLanguage.

    @Override
	public Language getLanguage(final String languageCode, final String countryCode) {
		return factory.getLanguage(languageCode, countryCode);
	}

	@Override
    public boolean isAssetTypeLanguage(final String id) {
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
	public Language getLanguage(final String id) {
		return factory.getLanguage(id);
	}

	@Override
	public Language createDefaultLanguage() {

        Language  language = null;

        try {

            language = wrapReturn(() -> factory.createDefaultLanguage());
            Logger.debug(this, "Created default language");
        } catch (DotDataException e) {

            if ( Logger.isErrorEnabled(LanguageAPIImpl.class) ) {
                Logger.error(LanguageAPIImpl.class, e.getMessage(), e);
            }
        }
		return language;
	}

	@Override
	public Language getLanguage(final long id) {
		return factory.getLanguage(id);
	}

	@Override
	public List<Language> getLanguages() {
		return factory.getLanguages();
	}

	@Override
	public void saveLanguage(final Language language) {
        try {

            wrap(() -> factory.saveLanguage(language));
            Logger.debug(this, "Created language: " + language);
        } catch (DotDataException e) {

            if ( Logger.isErrorEnabled(LanguageAPIImpl.class) ) {
                Logger.error(LanguageAPIImpl.class, e.getMessage(), e);
            }
        }
	}

	@Override
	public String getLanguageCodeAndCountry(final long id, final String langId) {
		return factory.getLanguageCodeAndCountry(id, langId);
	}

	@Override
	public Language getDefaultLanguage() {
		return factory.getDefaultLanguage();
	}

	@Override
	public boolean hasLanguage(final String id) {
		return factory.hasLanguage(id);
	}

	@Override
	public boolean hasLanguage(final long id) {
		return factory.hasLanguage(id);
	}

	@Override
	public boolean hasLanguage(final String languageCode, final String countryCode) {
		return factory.hasLanguage(languageCode, countryCode);
	}

	@Override
	public List<LanguageKey> getLanguageKeys(final String langCode) {
		final List<LanguageKey> list = factory.getLanguageKeys(langCode);
		Collections.sort(list, LANGUAGE_KEY_COMPARATOR);
		return list;
	}

	@Override
	public List<LanguageKey> getLanguageKeys(final String langCode, final String countryCode) {
		final List<LanguageKey> list = factory.getLanguageKeys(langCode, countryCode);
		Collections.sort(list, LANGUAGE_KEY_COMPARATOR);
		return list;
	}

	@Override
	public List<LanguageKey> getLanguageKeys(final Language lang) {
		final String langCode = lang.getLanguageCode();
        final String countryCode = lang.getCountryCode();
		final List<LanguageKey> list = new ArrayList<LanguageKey>(factory.getLanguageKeys(langCode));
		Collections.sort(list, LANGUAGE_KEY_COMPARATOR);

		final List<LanguageKey> keys = factory.getLanguageKeys(langCode, countryCode);
		for(LanguageKey key : keys) { // todo: analize it but it could be used an set instead of arraylist.
			int index = -1;
			if((index = Collections.binarySearch(list, key, LANGUAGE_KEY_COMPARATOR)) >= 0) {
				list.remove(index);
			}
			list.add(key);
		}

		Collections.sort(list, LANGUAGE_KEY_COMPARATOR);
		return list;
	}

	@Override
	public void createLanguageFiles(final Language lang) {

        try {

            wrap(() -> factory.createLanguageFiles(lang));
            Logger.debug(this, "Created language file for lang: " + lang);
        } catch (DotDataException e) {

            if ( Logger.isErrorEnabled(LanguageAPIImpl.class) ) {
                Logger.error(LanguageAPIImpl.class, e.getMessage(), e);
            }
        }

	}

	@Override
	public void saveLanguageKeys(final Language lang, final Map<String, String> generalKeys,
                                 final Map<String, String> specificKeys, final Set<String> toDeleteKeys) throws DotDataException {

		final List<LanguageKey> existingGeneralKeys  = getLanguageKeys(lang.getLanguageCode());
        final List<LanguageKey> existingSpecificKeys = getLanguageKeys(lang.getLanguageCode(),lang.getCountryCode());

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

        try {

            wrap(() -> factory.saveLanguageKeys(lang, generalKeys, specificKeys, toDeleteKeys));
            Logger.debug(this, "Created language file for lang: " + lang);
        } catch (DotDataException e) {

            if ( Logger.isErrorEnabled(LanguageAPIImpl.class) ) {
                Logger.error(LanguageAPIImpl.class, e.getMessage(), e);
            }
        }
	}

	@Override
    public String getStringKey ( final Language lang, final String key ) {

        final User user = getUser();
        // First, retrieve value from legacy Language Variables or the appropriate
        // Language.properties file
        final String value = this.getStringFromPropertiesFile(lang, key);
        // If not found, look it up using the new Language Variable API
        return (null == value || StringPool.BLANK.equals(value.trim()) || key.equals(value) )?
                getLanguageVariableAPI().getLanguageVariableRespectingFrontEndRoles(key, lang.getId(), user):
                value;
    }

    private String getStringFromPropertiesFile (final Language lang, final String key) {

        String value = null;

        try {
            value = LanguageUtil.get( new Locale( lang.getLanguageCode(), lang.getCountryCode() ), key );
        } catch ( LanguageException e ) {
            Logger.error( this, e.getMessage(), e );
        }

        return value;
    }

	private User getUser() {

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

		return user;
	}

	@Override
	public boolean getBooleanKey(final Language lang, final String key) {
		return Boolean.parseBoolean(getStringKey(lang, key));
	}

	@Override
	public boolean getBooleanKey(final Language lang, final String key, final boolean defaultVal) {
		return (getStringKey(lang, key) != null)?
			Boolean.parseBoolean(getStringKey(lang, key)):defaultVal;
	}

	@Override
	public float getFloatKey(final Language lang, final String key) {
		return Float.parseFloat(getStringKey(lang, key));
	}

	@Override
	public float getFloatKey(final Language lang, final String key, final float defaultVal) {
		return (getStringKey(lang, key) != null)?
			Float.parseFloat(getStringKey(lang, key)):defaultVal;
	}

	@Override
	public int getIntKey(final Language lang, final String key) {
		return Integer.parseInt(getStringKey(lang, key));
	}

	@Override
	public int getIntKey(final Language lang, final String key, final int defaultVal) {
		return (getStringKey(lang, key) != null)?
                Integer.parseInt(getStringKey(lang, key)):defaultVal;
	}

	@Override
	public void clearCache(){
		CacheLocator.getLanguageCache().clearCache();
	}

    @Override
    public Language getFallbackLanguage(final String languageCode) {
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

        	synchronized (this) {
				if (null == this.languageVariableAPI) {

					this.languageVariableAPI = APILocator.getLanguageVariableAPI();
				}
			}
        }

        return this.languageVariableAPI;
    }
    
}
