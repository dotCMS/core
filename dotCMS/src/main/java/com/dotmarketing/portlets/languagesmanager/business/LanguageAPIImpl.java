package com.dotmarketing.portlets.languagesmanager.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.languagevariable.business.LanguageVariable;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.DotPreconditions;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.DisplayedLanguage;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.velocity.tools.view.context.ViewContext;
import static com.dotmarketing.portlets.languagesmanager.business.LanguageAPI.isLocalizationEnhancementsEnabled;

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
	private final LocalSystemEventsAPI localSystemEventsAPI = APILocator.getLocalSystemEventsAPI();

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
        Logger.debug(this, ()-> "DeleteLanguage: " + language);

		try {
			HibernateUtil.addCommitListener(()-> {

				localSystemEventsAPI.asyncNotify(new LanguageDeletedEvent(language));
			});
		} catch (DotHibernateException e) {

			Logger.error(this, e.getMessage(), e);
		}
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
	@Deprecated
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
		DotPreconditions.checkArgument(language!=null, "Language can't be null");
		DotPreconditions.checkArgument(UtilMethods.isSet(language.getLanguageCode()),
				"Language Code can't be null or empty");
		DotPreconditions.checkArgument(UtilMethods.isSet(language.getLanguage()),
				"Language String can't be null or empty");


        factory.saveLanguage(language);
        Logger.info(this, "Created language: " + language);
	}

    @CloseDBIfOpened
	@Override
	public String getLanguageCodeAndCountry(final long id, final String langId) {
		return factory.getLanguageCodeAndCountry(id, langId);
	}

	/**
	 * This method does not need to be transactional as long as it reads from cache
	 * The relevant transactional bits are located in the factory layer that actually writes and creates the default lang
	 * @return
	 */
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
	@Deprecated(since = "24.05", forRemoval = true)
	public List<LanguageKey> getLanguageKeys(final Language lang) {
		final String langCode = lang.getLanguageCode();
        final String countryCode = lang.getCountryCode();

		final Set<LanguageKey>  list = new TreeSet<>(LANGUAGE_KEY_COMPARATOR);
		list.addAll(factory.getLanguageKeys(langCode));
		list.addAll(factory.getLanguageKeys(langCode, countryCode));

		return ImmutableList.copyOf(list);
	}

	@Override
    @WrapInTransaction
	@Deprecated(since = "24.05", forRemoval = true)
	public void createLanguageFiles(final Language lang) {

        this.factory.createLanguageFiles(lang);
        Logger.debug(this, "Created language file for lang: " + lang);
	}

	@WrapInTransaction
	@Override
	@Deprecated(since = "24.05", forRemoval = true)
	public void saveLanguageKeys(final Language lang, final Map<String, String> generalKeysIncoming,
                                 final Map<String, String> specificKeys, final Set<String> toDeleteKeys) throws DotDataException {

		final List<LanguageKey> existingGeneralKeys  = getLanguageKeys(lang.getLanguageCode());
        final List<LanguageKey> existingSpecificKeys = getLanguageKeys(lang.getLanguageCode(),lang.getCountryCode());

    final Map<String,String> generalKeys = new HashMap<>();
    generalKeys.putAll(generalKeysIncoming);
        
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
          Logger.error(LanguageAPIImpl.class, e.getMessage(), e);
        }
	}
	
  @CloseDBIfOpened
  @Override
  public Map<String, String> getStringsAsMap(final Locale locale, final Collection<String> keys) {
	final LanguageVariableAPI langVarsAPI = getLanguageVariableAPI();
    final Map<String, String> messagesMap = new HashMap<>();

    if (null != keys) {
      final Language lang = APILocator.getLanguageAPI().getLanguage(locale.getLanguage(), locale.getCountry());
      keys.forEach(messageKey -> {
		  Optional<LanguageVariable> variable = Optional.empty();
		  if(isLocalizationEnhancementsEnabled()) {
			 variable = Try.of(
							 () -> langVarsAPI.findVariable(lang.getId(), messageKey))
					 .getOrElse(Optional.empty());
			  variable.ifPresent(
					  languageVariable -> messagesMap.put(messageKey, languageVariable.value()));
		 }
		 // final code should always use first the LanguageVariableAPI to get the value of the key if not found then use the code below
		  if (variable.isEmpty()) {
			  //This code is still valid even if we decide to use the LanguageVariableAPI to get the value of the key
			  //The code below will still look for the key in the system properties
			  String message = (lang != null)
					  ? getStringKey(lang, messageKey)
					  : getStringFromPropertiesFile(locale, messageKey);
			  message = (message == null) ? messageKey : message;
			  messagesMap.put(messageKey, message);
		  }
      });
    }

    return messagesMap;
  }
	
	@CloseDBIfOpened
	@Override
    public String getStringKey ( final Language lang, final String key ) {
		final LanguageVariableAPI langVarsAPI = getLanguageVariableAPI();

		//Once we're ready to roll out the new Language Variables, we should remove this check
		if (isLocalizationEnhancementsEnabled()) {
			final Optional<LanguageVariable> variable = Try.of(
					() -> langVarsAPI.findVariable(lang.getId(), key)).getOrElse(Optional.empty());
			if (variable.isPresent()) {
				return variable.get().value();
			}
		}
        //This code will still be valid even if we decide to use the LanguageVariableAPI to get the value of the key
		final User user = getUser();
        // First, look it up using the new Language Variable API
        final String value = langVarsAPI.getLanguageVariableRespectingFrontEndRoles(key, lang.getId(), user);
        // If not found, retrieve value from legacy Language Variables or the appropriate
        final String countryCode = null == lang.getCountryCode()?"":lang.getCountryCode();
        return (UtilMethods.isNotSet(value) || value.equals(key)) ? this.getStringFromPropertiesFile(new Locale( lang.getLanguageCode(), countryCode ), key) : value;
    }

	/**
	 * This method internally uses MultiLanguageResourceBundle to get the value of the key.
	 * if the enhancedLanguageVariables is enabled, it will use the LanguageVariableAPI to get the value of the key.
	 * Therefore, it won't necessarily load the value from the properties file. And we should probably rename this method to getStringKeyFromLanguageVariable
	 * @param locale
	 * @param key
	 * @return
	 */
    private String getStringFromPropertiesFile (final Locale locale, final String key) {
        String value = null;
        try {
            value = LanguageUtil.get( locale, key );
        } catch ( LanguageException e ) {
            Logger.error( this, e.getMessage(), e );
        }
        return value;
    }

    @VisibleForTesting
	protected User getUser() {

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

	@CloseDBIfOpened
	@Override
	public Optional<Language> getFindFirstLanguageByCode(final String languageCode) {
		return this.factory.getFindFirstLanguageByCode(languageCode);
	}

	@Override
	@CloseDBIfOpened
	public List<Language> getAvailableContentLanguages(final String contentletInode, final User user)
			throws DotSecurityException, DotDataException {

		final Contentlet contentlet = APILocator.getContentletAPI().find(contentletInode, user, false);
		final List<DisplayedLanguage> availableContentPageLanguages = VelocityUtil.getAvailableContentPageLanguages(contentlet);

		return availableContentPageLanguages.stream()
				.map(displayedLanguage -> displayedLanguage.getLanguage())
				.collect(CollectionsUtils.toImmutableList());
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

	/**
	 * {@inheritDoc}
	 */
    @Override
    public boolean canDefaultContentToDefaultLanguage() {
        return Config.getBooleanProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE",false);
    }

	/**
	 * {@inheritDoc}
	 */
    @Override
    public boolean canDefaultWidgetToDefaultLanguage() {
        return Config.getBooleanProperty("DEFAULT_WIDGET_TO_DEFAULT_LANGUAGE",true);
    }

	/**
	 * {@inheritDoc}
	 */
    @Override
    public boolean canDefaultPageToDefaultLanguage () {
        return Config.getBooleanProperty( "DEFAULT_PAGE_TO_DEFAULT_LANGUAGE", true );
    }

	/**
	 * {@inheritDoc}
	 */
    @Override
    public boolean canDefaultFileToDefaultLanguage() {
        return Config.getBooleanProperty("DEFAULT_FILE_TO_DEFAULT_LANGUAGE",true);
    }

	/**
	 * {@inheritDoc}
	 * @return
	 */
	@WrapInTransaction
    @Override
	public Language makeDefault(Long languageId, final User user)
			throws DotDataException, DotSecurityException {
    	if(!user.isAdmin()){
    		throw new DotSecurityException("Only admin users are allowed to perform a default language switch.");
		}
		factory.makeDefault(languageId);
        return factory.getDefaultLanguage();
	}

	/**
	 * {@inheritDoc}
	 * @return
	 */
	@WrapInTransaction
	public void transferAssets(final Long oldDefaultLanguage, final Long newDefaultLanguage, final User user)
			throws DotDataException, DotIndexException, DotSecurityException {
		if(!user.isAdmin()){
			throw new DotSecurityException("Only admin users are allowed to perform a default language switch for assets");
		}
        factory.transferAssets(oldDefaultLanguage, newDefaultLanguage);
		HibernateUtil.addCommitListener(() -> {
			MaintenanceUtil.flushCache();
			APILocator.getContentletAPI().refreshAllContent();
		});
	}
}
