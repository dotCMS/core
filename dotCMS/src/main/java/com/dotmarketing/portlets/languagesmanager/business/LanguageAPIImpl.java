package com.dotmarketing.portlets.languagesmanager.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
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
    @WrapInTransaction
	public void deleteLanguage(final Language language) {

        this.factory.deleteLanguage(language);
        Logger.debug(this, "deleteLanguage");
	}

    @Override
    @WrapInTransaction
    public void deleteFallbackLanguage(final Language fallbackLanguage) {

	    final int rowsAffected =
                this.factory.deleteLanguageById(fallbackLanguage);
        Logger.debug(this, "deleteFallbackLanguage, rowsAffected: " + rowsAffected);
	} // deleteFallbackLanguage.

    @CloseDBIfOpened
    @Override
	public Language getLanguage(final String languageCode, final String countryCode) {
		return factory.getLanguage(languageCode, countryCode);
	}

    @CloseDBIfOpened
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

    @CloseDBIfOpened
	@Override
	public Language getLanguage(final String id) {
		return factory.getLanguage(id);
	}

	@Override
	@WrapInTransaction
	public Language createDefaultLanguage() {

        final Language  language =
				this.factory.createDefaultLanguage();
        Logger.debug(this, "Created default language");

		return language;
	}

	@CloseDBIfOpened
	@Override
	public Language getLanguage(final long id) {
		return factory.getLanguage(id);
	}

	@CloseDBIfOpened
	@Override
	public List<Language> getLanguages() {
		return factory.getLanguages();
	}

	@WrapInTransaction
	@Override
	public void saveLanguage(final Language language) {

        factory.saveLanguage(language);
        Logger.debug(this, "Created language: " + language);
	}

    @CloseDBIfOpened
	@Override
	public String getLanguageCodeAndCountry(final long id, final String langId) {
		return factory.getLanguageCodeAndCountry(id, langId);
	}

    @CloseDBIfOpened
	@Override
	public Language getDefaultLanguage() {
		return factory.getDefaultLanguage();
	}

    @CloseDBIfOpened
	@Override
	public boolean hasLanguage(final String id) {
		return factory.hasLanguage(id);
	}

    @CloseDBIfOpened
	@Override
	public boolean hasLanguage(final long id) {
		return factory.hasLanguage(id);
	}

    @CloseDBIfOpened
	@Override
	public boolean hasLanguage(final String languageCode, final String countryCode) {
		return factory.hasLanguage(languageCode, countryCode);
	}

    @CloseDBIfOpened
	@Override
	public List<LanguageKey> getLanguageKeys(final String langCode) {
		final List<LanguageKey> list = factory.getLanguageKeys(langCode);
		Collections.sort(list, LANGUAGE_KEY_COMPARATOR);
		return list;
	}

    @CloseDBIfOpened
	@Override
	public List<LanguageKey> getLanguageKeys(final String langCode, final String countryCode) {
		final List<LanguageKey> list = factory.getLanguageKeys(langCode, countryCode);
		Collections.sort(list, LANGUAGE_KEY_COMPARATOR);
		return list;
	}

    @CloseDBIfOpened
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
    @WrapInTransaction
	public void createLanguageFiles(final Language lang) {

        this.factory.createLanguageFiles(lang);
        Logger.debug(this, "Created language file for lang: " + lang);
	}

	@WrapInTransaction
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

            factory.saveLanguageKeys(lang, generalKeys, specificKeys, toDeleteKeys);
            Logger.debug(this, "Created language file for lang: " + lang);
        } catch (DotDataException e) {

            if ( Logger.isErrorEnabled(LanguageAPIImpl.class) ) {
                Logger.error(LanguageAPIImpl.class, e.getMessage(), e);
            }
        }
	}

	@CloseDBIfOpened
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
            value = LanguageUtil.get( new Locale( lang.getLanguageCode(), lang.getCountryCode()==null?"":lang.getCountryCode() ), key );
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

	@CloseDBIfOpened
	@Override
	public boolean getBooleanKey(final Language lang, final String key) {
		return Boolean.parseBoolean(getStringKey(lang, key));
	}

    @CloseDBIfOpened
	@Override
	public boolean getBooleanKey(final Language lang, final String key, final boolean defaultVal) {
		return (getStringKey(lang, key) != null)?
			Boolean.parseBoolean(getStringKey(lang, key)):defaultVal;
	}

    @CloseDBIfOpened
	@Override
	public float getFloatKey(final Language lang, final String key) {
		return Float.parseFloat(getStringKey(lang, key));
	}

    @CloseDBIfOpened
	@Override
	public float getFloatKey(final Language lang, final String key, final float defaultVal) {
		return (getStringKey(lang, key) != null)?
			Float.parseFloat(getStringKey(lang, key)):defaultVal;
	}

    @CloseDBIfOpened
	@Override
	public int getIntKey(final Language lang, final String key) {
		return Integer.parseInt(getStringKey(lang, key));
	}

    @CloseDBIfOpened
	@Override
	public int getIntKey(final Language lang, final String key, final int defaultVal) {
		return (getStringKey(lang, key) != null)?
                Integer.parseInt(getStringKey(lang, key)):defaultVal;
	}

	@Override
	public void clearCache(){
		CacheLocator.getLanguageCache().clearCache();
	}

	@CloseDBIfOpened
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
