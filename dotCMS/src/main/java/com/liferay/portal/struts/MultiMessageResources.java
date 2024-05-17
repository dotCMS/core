/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.liferay.portal.struts;

import static com.dotmarketing.portlets.languagesmanager.business.LanguageAPI.isLocalizationEnhancementsEnabled;

import com.dotcms.languagevariable.business.LanguageVariable;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.repackage.org.apache.struts.util.MessageResourcesFactory;
import com.dotcms.repackage.org.apache.struts.util.PropertyMessageResources;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.ChainableCacheAdministratorImpl;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.LanguageKey;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.util.StringUtil;
import io.vavr.control.Try;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="MultiMessageResources.java.html"><b><i>View Source </i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @version $Revision: 1.6 $
 */
public class MultiMessageResources extends PropertyMessageResources {

    private static final Log _log = LogFactory.getLog(MultiMessageResources.class);
    Map allMsgByLoc = new HashMap();
    private transient ServletContext _servletContext;

    public MultiMessageResources(MessageResourcesFactory factory, String config) {

        super(factory, config);
    }

    public MultiMessageResources(MessageResourcesFactory factory, String config, boolean returnNull) {

        super(factory, config, returnNull);
    }

    public Map getMessages() {
        synchronized (messages) {
            return ImmutableMap.copyOf(messages);
        }
    }
    // START-NOSCAN
    // Lifted this code directly from com.oroad.stxx.util.PropertyMessageResource.class v.1.3
    // which this class used to extend. This method does not exist in struts PropertyMessageResource
    public Map getMessages(Locale locale) {
        String localeKey = locale.toString();
        HashMap result = null;
        if (this.allMsgByLoc.containsKey(locale)) {
            if (log.isDebugEnabled()) {
                log.debug("Pulling messages for locale " + locale + " from the cache");
            }

            result = new HashMap((Map) this.allMsgByLoc.get(locale));
            return result;
        } else {
            if (log.isInfoEnabled()) {
                log.info("Loading all possible messages for locale " + locale);
            }

            result = new HashMap();

            while (true) {
                this.loadLocale(localeKey);
                int underscore = localeKey.lastIndexOf("_");
                if (underscore < 0) {
                    if (!this.defaultLocale.equals(locale)) {
                        localeKey = this.localeKey(this.defaultLocale);
                        this.loadLocale(localeKey);
                    }

                    localeKey = "";
                    this.loadLocale(localeKey);
                    Set keys = new HashSet();
                    HashMap var10 = this.messages;
                    String key;
                    Iterator i;
                    synchronized (var10) {
                        i = this.messages.entrySet().iterator();

                        while (i.hasNext()) {
                            Map.Entry entry = (Map.Entry) i.next();
                            key = (String) entry.getKey();
                            key = key.substring(key.indexOf(".") + 1, key.length());
                            keys.add(key);
                        }
                    }

                    i = keys.iterator();

                    while (true) {
                        String value;
                        do {
                            do {
                                if (!i.hasNext()) {
                                    this.allMsgByLoc.put(locale, Collections.unmodifiableMap(new HashMap(result)));
                                    return result;
                                }

                                key = (String) i.next();
                                value = this.getMessage(locale, key);
                            } while (value == null);
                        } while (this.returnNull && value.startsWith("???"));

                        result.put(key, value);
                    }
                }

                localeKey = localeKey.substring(0, underscore);
            }
        }
    }
    // END-NOSCAN

    public void setServletContext(ServletContext servletContext) {
        _servletContext = servletContext;
    }

    @Override
    protected synchronized void loadLocale(String localeKey) {

        synchronized (locales) {
            if (locales.get(localeKey) != null) {
                return;
            }

            locales.put(localeKey, localeKey);
        }

        // Load the properties files for the specified locale
        final String[] names = StringUtil.split(config.replace('.', '/'));

        for (String name : names) {
            if (localeKey.length() > 0) {
                name += "_" + localeKey;
            }
            name += ".properties";
            internalLoadProps(name, localeKey);
        }

        // Load the language variables if the localization enhancements are enabled
        if (isLocalizationEnhancementsEnabled()) {
            final LanguageVariableAPI languageVariableAPI = APILocator.getLanguageVariableAPI();
            final long languageId = LanguageUtil.getLanguageId(localeKey, false);
            if (languageId > 0) {
                final List<LanguageVariable> variables = Try.of(
                        () -> languageVariableAPI.findVariables(languageId)).getOrElse(List.of());
                if (variables.isEmpty()) {
                    return;
                }
                Logger.debug(this, "Loading language variables for locale: " + localeKey);
                synchronized (messages) {
                    for (LanguageVariable variable : variables) {
                        putOrUpdate(messages, messageKey(localeKey, variable.key()), variable.value());
                    }
                }
            }
        }

    }

    /**
     * Load the properties file
     * @param name the name of the properties file
     * @param localeKey the locale key
     */
    private void internalLoadProps(String name, String localeKey) {
        //These are user provided properties, and we should only look at them if the localization enhancements are disabled
        if (name.contains("cms_language") && !isLocalizationEnhancementsEnabled()) {
            loadUserProvidedKeys(localeKey);
        } else {
            // These source base properties are not user provided, we always want to load them
            loadSystemProperties(name, localeKey);
        }
    }

    /**
     * Load the system properties meaning all property files provided in our source base that end up living in the WEB-INF directory
     * @param name the name of the properties file
     * @param localeKey the locale key
     */
    private void loadSystemProperties(String name, String localeKey) {
        final Properties props = new Properties();
        try {
            final URL url = _servletContext.getResource("/WEB-INF/" + name);
            if (url != null) {
                try (InputStream is = url.openStream(); BufferedReader buffy = new BufferedReader(new InputStreamReader(is))) {
                    parseProps(props, buffy);
                }
            }
        } catch (Exception e) {
            Logger.error(this, "Error loading system properties", e);
        }

        if (props.isEmpty()) {
            return;
        }

        synchronized (messages) {
            final Enumeration<?> names = props.keys();

            while (names.hasMoreElements()) {
                String key = (String) names.nextElement();

                messages.put(messageKey(localeKey, key), props.getProperty(key));
            }
        }
    }

    /**
     * Load user provided keys from the database
     * @param localeKey the locale key
     * @param langAPI the language API
     */
    private void loadUserProvidedKeys(String localeKey) {
        final LanguageAPI langAPI = APILocator.getLanguageAPI();
        List<LanguageKey> keys;
        if (localeKey.split("_").length > 1) {
            keys = langAPI.getLanguageKeys(localeKey.split("_")[0], localeKey.split("_")[1]);
        } else {
            keys = langAPI.getLanguageKeys(localeKey.split("_")[0]);
        }

        if (keys.isEmpty()) {
            return;
        }

        synchronized (messages) {
            Iterator<LanguageKey> names = keys.iterator();

            while (names.hasNext()) {
                LanguageKey langKey = names.next();
                String key = langKey.getKey();
                messages.put(messageKey(localeKey, key), langKey.getValue());
            }
        }
    }

    /**
     * Parses the properties from the given buffered reader and puts them into the given properties object.
     * @param props the properties object
     * @param buffy the buffered reader
     * @throws IOException if an error occurs
     */
    private void parseProps(Map<Object,Object> props, BufferedReader buffy) throws IOException {
        String line = null;
        while ((line = buffy.readLine()) != null) {
            if (UtilMethods.isSet(line) && line.contains("=") && !line.startsWith("#")) {
                final String[] arr = line.split("=", 2);
                if (arr.length > 1) {
                    String key = arr[0].trim();
                    String val = arr[1].trim();
                    if (val.contains("\\u")) {
                        val = parseVal(val);
                    }
                    putOrUpdate(props, key, val);
                }
            }
        }
    }

    /**
     * Parses the given value and returns the parsed value.
     * @param val the value
     * @return the parsed value
     */
    private static String parseVal(String val) {
        StringBuilder buffer = new StringBuilder(val.length());
        boolean precedingBackslash = false;
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (precedingBackslash) {
                switch (c) {
                    case 'f':
                        c = '\f';
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'u':
                        String hex = val.substring(i + 1, i + 5);
                        c = (char) Integer.parseInt(hex, 16);
                        i += 4;
                        break;
                    default:
                        break;
                }
                precedingBackslash = false;
            } else {
                precedingBackslash = (c == '\\');
            }
            if (!precedingBackslash) {
                buffer.append(c);
            }
        }
        val = buffer.toString();
        return val;
    }

    /**
     * Puts or updates a property in the given properties object.
     * @param props the properties object
     * @param key the key
     * @param val the value
     */
    private void putOrUpdate(final Map<Object,Object> props, final String key, final String val) {
        props.compute(key, (k, existingVal) -> {
            if (existingVal != null) {
                Logger.warn(this.getClass(), String.format(
                        "Duplicate resource property definition (key=was ==> is now): %s=%s ==> %s",
                        key, existingVal, val));
            }
            return val;
        });
    }

    public synchronized void reload() {
        reloadLocally();

        ChainableCacheAdministratorImpl dotCache = ((ChainableCacheAdministratorImpl) CacheLocator.getCacheAdministrator()
                .getImplementationObject());
        if (dotCache.isClusteringEnabled()) {
            dotCache.send("MultiMessageResources.reload");
        }
    }

    public void reloadLocally() {
        locales.clear();
        messages.clear();
        formats.clear();
    }

}
