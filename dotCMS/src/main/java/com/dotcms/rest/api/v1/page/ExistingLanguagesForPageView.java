package com.dotcms.rest.api.v1.page;

import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This View is used to indicate what language an HTML Page is available on, and what language it is not. This is
 * particularly useful for the UI to be able to know when a page is NOT available in a specific language, and then
 * provide the user the option to create it.
 *
 * @author Jose Castro
 * @since Jan 5th, 2023
 *
 * @deprecated This class is deprecated and will be removed in a future version of dotCMS.
 */
@Deprecated(since = "Nov 7th, 24", forRemoval = true)
public class ExistingLanguagesForPageView extends HashMap<String, Object> implements Serializable {

    public ExistingLanguagesForPageView(final Language language, final boolean translated) {
        final Map<String, Object> dataMap = new HashMap<>(language.toMap());
        this.putAll(dataMap);
        this.put("translated", translated);
    }

}
