package com.dotcms.util;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.util.LocaleUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Encapsulates i18n stuff
 * @author jsanca
 */
public class I18NUtil implements Serializable {

    public static final I18NUtil INSTANCE = new I18NUtil();

    private I18NUtil() {}

    /**
     * Get a map of messages from a locale (country + lang) and a collection of message key, the result will be a map where the key is the message key and the value the i18n message.
     *
     * In case a message key can not be found, will return the message key as message value.
     *
     *
     * @param country
     * @param lang
     * @param messagesKey
     * @param request
     *
     * @return Map
     */
    public Map<String, String> getMessagesMap (final String country, final String lang, final Collection<String> messagesKey, final HttpServletRequest request) {

        return this.getMessagesMap (country, lang, messagesKey, request, false);
    } // getMessagesMap

    /**
     * Get a map of messages from a locale (country + lang) and a collection of message key, the result will be a map where the key is the message key and the value the i18n message.
     *
     * In case a message key can not be found, will return the message key as message value.
     *
     *
     * @param country
     * @param lang
     * @param messagesKey
     * @param request
     *
     * @return Map
     */
    public Map<String, String> getMessagesMap (final String country, final String lang,
                                               final Collection<String> messagesKey,
                                               final HttpServletRequest request,
                                               final boolean createSession) {

        final Locale locale = LocaleUtil.getLocale(request,
                country, lang, createSession);

        return this.getMessagesMap(locale, messagesKey);
    } // getMessagesMap

    /**
     * Get a map of messages from a locale and a collection of message key, the result will be a map where the key is the message key and the value the i18n message.
     *
     * In case a message key can not be found, will return the message key as message value.
     *
     * @param locale
     * @param messagesKey
     *
     * @return Map
     */
    public Map<String, String> getMessagesMap (final Locale locale, final Collection<String> messagesKey) {

        final Map<String, String> messagesMap = map();

        if (null != messagesKey) {
            final Language lang = APILocator.getLanguageAPI().getLanguage(locale.getLanguage(), locale.getCountry());
            messagesKey.forEach(
                    messageKey -> {

                        try {
                            
                            messagesMap.put(messageKey,
                                    APILocator.getLanguageAPI().getStringKey(lang, messageKey));
                        } catch (Exception e) {

                            messagesMap.put(messageKey,
                                    messageKey);
                        }
                    }
            );
        }

        return messagesMap;
    } // getMessagesMap.

    /**
     * Get a map of messages from a locale and a collection of message key, the result will be a map where the key is the message key and the value the i18n message.
     *
     * In case a message key can not be found, will return the message key as message value.
     *
     * @param locale
     * @param messagesKey
     *
     * @return Map
     */
    public Map<String, String> getMessagesMap (final Locale locale, final String... messagesKey) {

        return this.getMessagesMap(locale, Arrays.asList(messagesKey));
    } // getMessagesMap.

} // E:O:F:I18NUtil.
