package com.dotcms.rest.api.v1.content;

import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Contentlet helper.
 * @author jsanca
 */
public class ContentletHelper implements Serializable {

    public static final ContentletHelper INSTANCE = new ContentletHelper();

    /**
     * Get Str Type Names
     * @param locale {@link Locale}
     * @return Map (type Id -> i18n value)
     * @throws LanguageException
     */
    public final Map<Integer, String> getStrTypeNames(final Locale locale) throws LanguageException {

        return map(
                1, LanguageUtil.get(locale, "Content"),
                2, LanguageUtil.get(locale, "Widget"),
                3, LanguageUtil.get(locale, "Form"),
                4, LanguageUtil.get(locale, "File"),
                5, LanguageUtil.get(locale, "HTMLPage"),
                6, LanguageUtil.get(locale, "Persona")
        );
    } // getStrTypeNames.

} // E:O:F:ContentletHelper.
