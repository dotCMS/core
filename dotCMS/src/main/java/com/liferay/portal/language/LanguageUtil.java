/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.language;

import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotcms.repackage.org.apache.struts.taglib.TagUtils;
import com.dotcms.repackage.org.apache.struts.util.MessageResources;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebAppPool;
import com.liferay.util.CollectionFactory;
import com.liferay.util.GetterUtil;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import com.liferay.util.Time;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotmarketing.util.UtilMethods.isNotSet;

/**
 * <a href="LanguageUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author  Brian Wing Shun Chan
 * @version $Revision: 1.12 $
 *
 */
public class LanguageUtil {

	public static final String DEFAULT_ENCODING = "UTF-8";

	private static final Pattern DASH_PATTERN = Pattern.compile("^[a-zA-Z0-9]+-[a-zA-Z0-9]+$");
	private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("^[a-zA-Z0-9]+_[a-zA-Z0-9]+$");

	/**
	 * Returns the locale (if could) from the string representation: could be a language code, language code + country (en, en-US, es_US)
	 * @param languageTag {@link String} ilang code or lang code + country code
	 * @return locale otherwise DoesNotExistException
	 */
	public static Locale validateLanguageTag(final String languageTag) throws DoesNotExistException {
		final Locale locale = Locale.forLanguageTag(languageTag);
		final boolean validCountry = (isNotSet(locale.getCountry()) || Stream.of(Locale.getISOCountries()).collect(Collectors.toSet()).contains(locale.getCountry()));
		final boolean validLang = Stream.of(Locale.getISOLanguages()).collect(Collectors.toSet()).contains(locale.getLanguage());
		if(validLang && validCountry) {
			return locale;
		} else {
			throw new DoesNotExistException(String.format(" `%s` is an invalid language tag ", languageTag));
		}
	}

	/**
	 * Returns the language id from the string representation: could be a long as a string, or could be a language code, language code + country (en, en-US, es_US)
	 * @param languageIdORCountryCode {@link String} id or lang code or lang code + country code
	 * @return long -1 if not possible to figure out
	 */
	public static long getLanguageId (final String languageIdORCountryCode){
		return internalGetLanguageId(languageIdORCountryCode, true);
	}

	/**
	 * Returns the language id from the string representation: could be a long as a string, or could be a language code, language code + country (en, en-US, es_US)
	 * @param languageIdORCountryCode {@link String} id or lang code or lang code + country code
	 * @param fallbackByBaseLan {@link boolean} if true, it will try to get the language by the base language
	 * @return long -1 if not possible to figure out
	 */
	public static long getLanguageId (final String languageIdORCountryCode, final boolean fallbackByBaseLan){
		return internalGetLanguageId(languageIdORCountryCode, fallbackByBaseLan);
	}

	/**
	 * Returns the language id from the string representation: could be a long as a string, or could be a language code, language code + country (en, en-US, es_US)
	 * @param languageIdORCountryCode {@link String} id or lang code or lang code + country code
	 * @param fallbackByBaseLang {@link boolean} if true, it will try to get the language by the base language
	 * @return long -1 if not possible to figure out
	 */
	private static long internalGetLanguageId(final String languageIdORCountryCode, final boolean fallbackByBaseLang) {

		long languageId = -1;
		if (UtilMethods.isSet(languageIdORCountryCode)) {
			//Try to get the language by the id
			languageId = ConversionUtils.toLong(languageIdORCountryCode, -1L);
			//No language id, try to get the language by the language code
			if (-1 == languageId) {
				final Tuple2<String, String> languageCountryCode = getLanguageCountryCodes(languageIdORCountryCode);
				if (null != languageCountryCode._1) {
					languageId = internalGetLanguageId(languageId, languageCountryCode._1, languageCountryCode._2, fallbackByBaseLang);
				}
			}
		}
		return languageId;
	}

	/**
	 * Returns the language id from the string representation: could be a long as a string, or could be a language code, language code + country (en, en-US, es_US)
	 * @param languageId {@link long} id
	 * @param languageCode {@link String} language code
	 * @param countryCode {@link String} country code
	 * @param fallbackByBaseLang {@link boolean} if true, it will try to get the language by the base language
	 * @return long -1 if not possible to figure out
	 */
	private static long internalGetLanguageId(long languageId, final String languageCode, final String countryCode, final boolean fallbackByBaseLang) {
		final LanguageAPI languageAPI = APILocator.getLanguageAPI();
		Language language = languageAPI.getLanguage(languageCode, countryCode);

		//Attempt fallback by base lang
		if ((null == language || language.getId() <= 0) && (UtilMethods.isSet(languageCode) && fallbackByBaseLang)) {
				language = languageAPI.getFallbackLanguage(languageCode);
		}
		if (null != language && language.getId() > 0) {
			languageId = language.getId();
		}
		return languageId;
	}

	/**
	 * Returns the language and country code from the string representation: could be a language
	 * code, language code + country code. E.g.: en, en-US, es_US, etc.
	 *
	 * @param languageCountryCode The language code or lang code + country code.
	 *
	 * @return A {@link Tuple2<String, String>} object with the language code and country code.
	 */
	public static Tuple2<String, String> getLanguageCountryCodes(final String languageCountryCode) {

		String languageCode 		   = languageCountryCode;
		String countryCode  		   = null;
		String [] languageCountryCodes = null;

		//More robust way to identify the language and country code
		//negative numbers were triggering a language code db lookup
		final Matcher dashMatcher = DASH_PATTERN.matcher(languageCountryCode);
		final Matcher underscoreMatcher = UNDERSCORE_PATTERN.matcher(languageCountryCode);

		if (dashMatcher.matches()) {

			languageCountryCodes = languageCountryCode.split(StringPool.DASH);

		} else if (underscoreMatcher.matches()) {

			languageCountryCodes = languageCountryCode.split(StringPool.UNDERLINE);
		}

		if (null != languageCountryCodes && languageCountryCodes.length >= 2) {

			languageCode = languageCountryCodes[0];
			countryCode  = languageCountryCodes[1];
		}

		return Tuple.of(languageCode, countryCode);
	}

    /**
     * Returns an internationalized value for a given kay and user
     *
     * @param user
     * @param key
     * @return
     * @throws LanguageException
     */
    public static String get ( User user, String key ) throws LanguageException {

        if ( user == null ) {
            try {
                user = PublicCompanyFactory.getDefaultCompany().getDefaultUser();
            } catch ( Exception e ) {
                Logger.error( LanguageUtil.class, "cannot find default user" );
            }
        }

        String companyId = (user.getCompanyId() == null || user.getCompanyId().equals( User.DEFAULT )) ? PublicCompanyFactory.getDefaultCompanyId() : user.getCompanyId();
        return get( companyId, user.getLocale(), key );
    }

	public static String get(Locale locale, String key) throws LanguageException {
		return get(PublicCompanyFactory.getDefaultCompanyId(), locale, key);
	}


	/**
	 * gets the key in the language set in the defaultCompany
	 * @param key
	 * @return
	 * @throws LanguageException
	 */
    public static String get(String key)
            throws LanguageException {
            return get(PublicCompanyFactory.getDefaultCompany(),key);
    }

	/**
	 * Get the i18n message based on the locale and the key (the message should be in the Language.properties, or the specific language file)
	 * In addition if you have placeholders such as {0}, {1}, etc in order to interpolate arguments, you can use the arguments parameter in order to
	 * send as much as you need.
	 * @param key    {@link String}
	 * @param arguments {@link Object} array
	 * @return String
	 * @throws LanguageException
	 */
	public static String get(final String key,
							 final Object... arguments) throws LanguageException {

		final String i18nMessage = get(PublicCompanyFactory.getDefaultCompany(),key);

		return  (null != arguments && arguments.length > 0)?
				MessageFormat.format(i18nMessage, arguments):
				i18nMessage;
	} // get

    /**
     * Gets the i18n message based on the locale and the key (the message should be in the Language.properties, or the
     * specific language file). In addition, if you have placeholders such as {0}, {1}, etc in order to interpolate
     * arguments. You can use the arguments parameter in order to send as much as you need.
     *
     * @param user      The {@link User} that determines the localization settings.
     * @param key       The message key in the properties file.
     * @param arguments The value(s) for the message placeholders.
     *
     * @return The i18n'ed message.
     *
     * @throws LanguageException An error occurred when accessing the i18n resource files.
     */
    public static String get(User user, final String key, final Object... arguments) throws LanguageException {
        if (null == user) {
            try {
                user = PublicCompanyFactory.getDefaultCompany().getDefaultUser();
            } catch (final Exception e) {
                Logger.error(LanguageUtil.class, "Cannot find default user.");
            }
        }
        final String companyId = (user.getCompanyId() == null || user.getCompanyId().equals(User.DEFAULT)) ?
                PublicCompanyFactory.getDefaultCompanyId() : user.getCompanyId();
        final String i18nMessage = get(companyId, user.getLocale(), key);
        return (null != arguments && arguments.length > 0) ? MessageFormat.format(i18nMessage, arguments) : i18nMessage;
    }

	/**
	 * Get the i18n message based on the locale and the key (the message should be in the Language.properties, or the specific language file)
	 * In addition if you have placeholders such as {0}, {1}, etc in order to interpolate arguments, you can use the arguments parameter in order to
	 * send as much as you need.
	 * @param locale {@link Locale}
	 * @param key    {@link String}
	 * @param arguments {@link Object} array
	 * @return String
	 * @throws LanguageException
     */
	public static String get(final Locale locale,
							 final String key,
							 final Object... arguments) throws LanguageException {

		final String i18nMessage = get(PublicCompanyFactory.getDefaultCompanyId(), locale, key);

		return  (null != arguments && arguments.length > 0)?
			MessageFormat.format(i18nMessage, arguments):
				i18nMessage;
	} // get

	public static String get(Company company, String key)
	throws LanguageException {
		if(company ==null){
			return null;
		}
		String value = null;
		Logger.debug(LanguageUtil.class, key);
		try {
			MessageResources resources = (MessageResources)WebAppPool.get(
				company.getCompanyId(), Globals.MESSAGES_KEY);
			
			if (resources != null)
				value = resources.getMessage(company.getLocale(), key);
		}
		catch (Exception e) {
			throw new LanguageException(e);
		}
	
		if (value == null) {
			Logger.debug(LanguageUtil.class, key);
			value = key;
		}
	
		return value;
	}

    public static String get(String companyId, Locale locale, String key) throws LanguageException {
        Optional<String> optValue = getOpt(companyId, locale, key);
        if(optValue.isEmpty()) {
            Logger.debug(LanguageUtil.class, key);
        }
        return optValue.orElse(key);
    }

    /**
     * A slight variation on getOpt({defaultCompany}, Locale, String) that will return optional.empty even in the
     * event of an exception, rather than declaring a checked exception that doesn't actually seem to ever actually
     * be thrown, except in the event of some odd server error well beyond our control.
     */
    public static MultiMessageResources getMessagesForDefaultCompany(Locale locale, String key) {
        return (MultiMessageResources)WebAppPool.get(PublicCompanyFactory.getDefaultCompanyId(), Globals.MESSAGES_KEY);
    }

	/**
	 * This method returns a map of all the messages according the locale.
	 *
	 * @param locale locale to search the messages
	 * @return
	 */
	public static Map getAllMessagesByLocale(final Locale locale){
    	return MultiMessageResources.class.cast(WebAppPool.get(PublicCompanyFactory.getDefaultCompanyId(), Globals.MESSAGES_KEY)).getMessages(locale);
	}

    public static Locale getDefaultCompanyLocale(){
        return PublicCompanyFactory.getDefaultCompany().getLocale();
    }

    public static Optional<String> getOpt(String companyId, Locale locale, String key) throws LanguageException {
        Optional<String> value = Optional.empty();
        Logger.debug(LanguageUtil.class, key);
        try {
            MessageResources resources = (MessageResources)WebAppPool.get(companyId, Globals.MESSAGES_KEY);

            if(resources != null) {
                value = Optional.ofNullable(resources.getMessage(locale, key));
            }
        } catch (Exception e) {
            throw new LanguageException(e);
        }

        if(value.isEmpty()) {
            Logger.debug(LanguageUtil.class, key);
        }

        return value;
    }

	public static String get(PageContext pageContext, String key) throws LanguageException {
    	return get(pageContext, key, (Object[])null);
	}
	
    public static String getOrDefaultValue(final String key, final String defaultValue) throws LanguageException {
        final User user = PortalUtil.getUser();
        
        final Optional<String> optValue = getOpt(APILocator.getCompanyAPI().getDefaultCompany().getCompanyId(), user.getLocale(), key);
        
        return optValue.orElse(defaultValue);

    }
	
	
	public static String get(PageContext pageContext, String key, Object... args)
		throws LanguageException {
		Logger.debug(LanguageUtil.class, key);
		String value = null;

		try {
			value = TagUtils.getInstance().message(
				pageContext, null, null, key, args);
		}
		catch (Exception e) {
			_log.error(e.getMessage());

			throw new LanguageException(key, e);
		}

		if (value == null) {
			Logger.debug(LanguageUtil.class, key);
			value = key;
		}

		return value;
	}

	public static Locale[] getAvailableLocales() {
		return _getInstance()._locales;
	}

    public static String getCharset(Locale locale) {
		return _getInstance()._getCharset(locale);
	}

	public static Locale getLocale(String languageCode) {
		return _getInstance()._getLocale(languageCode);
	}

	public static String format(
			PageContext pageContext, String pattern, Object argument)
		throws LanguageException {

		return format(pageContext, pattern, new Object[] {argument}, true);
	}
	
	public static String format(
			Locale locale, String pattern, Object argument)
		throws LanguageException {

		return format(locale, pattern, new Object[] {argument}, true);
	}

	public static String format(
			PageContext pageContext, String pattern, Object argument,
			boolean translateArguments)
		throws LanguageException {

		return format(
			pageContext, pattern, new Object[] {argument}, translateArguments);
	}
	
	public static String format(
			Locale locale, String pattern, Object argument,
			boolean translateArguments)
		throws LanguageException {

		return format(
				locale, pattern, new Object[] {argument}, translateArguments);
	}
	
	public static String format(
			PageContext pageContext, String pattern, Object[] arguments)
		throws LanguageException {

		return format(pageContext, pattern, arguments, true);
	}

	public static String format(
			PageContext pageContext, String pattern, Object[] arguments,
			boolean translateArguments)
		throws LanguageException {

		String value = null;
		String pattern2 = get(pageContext, pattern);
		if(!pattern.equals(pattern2)){
			pattern = pattern2;
		}
		try {
			Logger.debug(LanguageUtil.class, pattern);

			if (arguments != null) {
				Object[] formattedArguments = new Object[arguments.length];

				for (int i = 0; i < arguments.length; i++) {
					if (translateArguments) {
						formattedArguments[i] =
							get(pageContext, arguments[i].toString());
					}
					else {
						formattedArguments[i] = arguments[i];
					}
				}

				value = MessageFormat.format(pattern, formattedArguments);
			}
			else {
				Logger.warn(LanguageUtil.class, pattern);
				value = pattern;
			}
		}
		catch (Exception e) {
			throw new LanguageException(e);
		}

		return value;
	}
	
	public static String format(
			Locale locale, String pattern, String[] arguments) throws LanguageException{
		
		List<LanguageWrapper> lw = new ArrayList<>();
		for(int i=0;i< arguments.length;i++){
			
			lw.add(new LanguageWrapper("", arguments[i], ""));
		}
		
		
		
		
		
		
		return format(locale, pattern, (LanguageWrapper[]) lw.toArray(new LanguageWrapper[lw.size()]),false); 
	}
	
	
	
	
	public static String format(
			Locale locale, String pattern, Object[] arguments,
			boolean translateArguments)
		throws LanguageException {

		String value = null;
		User fakeUser = new User();
		fakeUser.setLocale(locale);
		String pattern2 = get(fakeUser, pattern);
		if(!pattern.equals(pattern2)){
			pattern = pattern2;
		}
		try {
			Logger.debug(LanguageUtil.class, pattern);

			if (arguments != null) {
				Object[] formattedArguments = new Object[arguments.length];

				for (int i = 0; i < arguments.length; i++) {
					if (translateArguments) {
						formattedArguments[i] =
							get(fakeUser, arguments[i].toString());
					}
					else {
						formattedArguments[i] = arguments[i];
					}
				}

				value = MessageFormat.format(pattern, formattedArguments);
			}
			else {
				Logger.warn(LanguageUtil.class, pattern);
				value = pattern;
			}
		}
		catch (Exception e) {
			throw new LanguageException(e);
		}

		return value;
	}
	
	public static String format(
			PageContext pageContext, String pattern, LanguageWrapper argument)
		throws LanguageException {

		return format(
			pageContext, pattern, new LanguageWrapper[] {argument}, true);
	}
	public static String format(
			Locale locale, String pattern, LanguageWrapper argument)
		throws LanguageException {

		return format(
				locale, pattern, new LanguageWrapper[] {argument}, true);
	}
	
	public static String format(
			PageContext pageContext, String pattern, LanguageWrapper argument,
			boolean translateArguments)
		throws LanguageException {

		return format(
			pageContext, pattern, new LanguageWrapper[] {argument},
			translateArguments);
	}

	public static String format(
			PageContext pageContext, String pattern,
			LanguageWrapper[] arguments)
		throws LanguageException {

		return format(pageContext, pattern, arguments, true);
	}

	public static String format(
			PageContext pageContext, String pattern,
			LanguageWrapper[] arguments, boolean translateArguments)
		throws LanguageException {

		String value = null;

		try {
			String pattern2 = get(pageContext, pattern);
			if(!pattern.equals(pattern2)){
				pattern = pattern2;
			}

			if (arguments != null) {
				Object[] formattedArguments = new Object[arguments.length];

				for (int i = 0; i < arguments.length; i++) {
					if (translateArguments) {
						formattedArguments[i] =
							arguments[i].getBefore() +
							get(pageContext, arguments[i].getText()) +
							arguments[i].getAfter();
					}
					else {
						formattedArguments[i] =
							arguments[i].getBefore() +
							arguments[i].getText() +
							arguments[i].getAfter();
					}
				}

				value = MessageFormat.format(pattern, formattedArguments);
			}
			else {
				value = pattern;
			}
		}
		catch (Exception e) {
			throw new LanguageException(e);
		}

		return value;
	}

	public static String getTimeDescription(
			PageContext pageContext, Long milliseconds)
		throws LanguageException {

		return getTimeDescription(pageContext, milliseconds.longValue());
	}

	public static String getTimeDescription(
			PageContext pageContext, long milliseconds)
		throws LanguageException {

		String desc = Time.getDescription(milliseconds);

		String value = null;

		try {
			int pos = desc.indexOf(" ");

			int x = GetterUtil.get(desc.substring(0, pos), 0);

			value =
				x + " " +
				get(
					pageContext,
					desc.substring(pos + 1, desc.length()).toLowerCase());
		}
		catch (Exception e) {
			throw new LanguageException(e);
		}

		return value;
	}

	public static Locale getLocale(PageContext pageContext) {
		return (Locale)pageContext.getSession().getAttribute(
			Globals.LOCALE_KEY);
	}

	private static LanguageUtil _getInstance() {
		if (_instance == null) {
			synchronized (LanguageUtil.class) {
				if (_instance == null) {
					_instance = new LanguageUtil();
				}
			}
		}

		return _instance;
	}

	private LanguageUtil() {
		String[] array = StringUtil.split(
			PropsUtil.get(PropsUtil.LOCALES), StringPool.COMMA);

		_locales = new Locale[array.length];
		_localesByLanguageCode = CollectionFactory.getHashMap();
		_charEncodings = CollectionFactory.getHashMap();

		for (int i = 0; i < array.length; i++) {
			int x = array[i].indexOf(StringPool.UNDERLINE);

			String language = array[i].substring(0, x);
			String country = array[i].substring(x + 1, array[i].length());

			Locale locale = new Locale(language, country);
			_locales[i] = locale;
			_localesByLanguageCode.put(language, locale);
			_charEncodings.put(locale.toString(), DEFAULT_ENCODING);
		}
	}

    private String _getCharset(Locale locale) {
		return DEFAULT_ENCODING;
	}

    private Locale _getLocale(String languageCode) {
		return (Locale)_localesByLanguageCode.get(languageCode);
	}

	private static final Log _log = LogFactory.getLog(LanguageUtil.class);

	private static LanguageUtil _instance;

	private Locale[] _locales;
	private Map _localesByLanguageCode;
	private Map _charEncodings;

    private static class MessageResult {}

	/**
	 * Search a Httpsession's attribute named {@link Globals#LOCALE_KEY}, if it exists then return it,
	 * if it doesn't exists then return the default user's locale and set it as a session's attribute.
	 *
	 * @param req
	 * @return The default {@link Locale}
	 *
	 * @see LanguageUtil#getDefaultCompanyLocale()
     */
	public static Locale getDefaultLocale(HttpServletRequest req){
		HttpSession session = req.getSession();
		Locale defaultLocale = (Locale) session.getAttribute(Globals.LOCALE_KEY);

		if (defaultLocale == null) {
			defaultLocale = getDefaultCompanyLocale();
			session.setAttribute(Globals.LOCALE_KEY, defaultLocale);
		}

		return defaultLocale;
	}
	
	/**
	 * Returns a String with the languageCode and the CountryCode appended, if there is no 
	 * CountryCode only the languageCode. This is use to search the flags that shows in the UI.
	 * 
	 * @param languageCode
	 * @param countryCode
	 * @return
	 */
    public static String getLiteralLocale(final String languageCode, final String countryCode) {
        final String newCountryCode = (countryCode == null || countryCode.isEmpty()) ? "" : "_"+countryCode;
        return new StringBuilder(languageCode).append(newCountryCode).toString();
    }

    public static Language getUserLanguage(final Language lang, final Locale locale) {
        Language userLanguage = lang;
        if(lang.getCountryCode()==null || lang.getCountryCode().isEmpty()){
            String defaultCountryCode = locale.getCountry();
            userLanguage = APILocator.getLanguageAPI().getLanguage(lang.getLanguageCode(), defaultCountryCode);
            userLanguage = (userLanguage != null && userLanguage.getId()>0)?userLanguage:lang;
        }
        return userLanguage;
    }
}
