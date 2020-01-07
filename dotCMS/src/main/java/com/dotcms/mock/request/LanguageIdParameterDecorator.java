package com.dotcms.mock.request;

import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageUtil;

/**
 * Decorates the languageId query string parameter in order to support things such as es, es_ES, en_EN, etc instead of just number or uuid,
 * @author jsanca
 */
public class LanguageIdParameterDecorator implements ParameterDecorator {

    @Override
    public String key() {
        return WebKeys.LANGUAGE_ID_PARAMETER;
    }

    @Override
    public String decorate(final String language) {
        return String.valueOf(LanguageUtil.getLanguageId(language));
    }
}

