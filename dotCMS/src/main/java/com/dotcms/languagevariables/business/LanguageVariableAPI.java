package com.dotcms.languagevariables.business;

import com.liferay.portal.model.User;

/**
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 27, 2017
 *
 */
public interface LanguageVariableAPI {

    public String get(final String key, final long languageId, final User user, final boolean respectFrontEnd);
    
}
