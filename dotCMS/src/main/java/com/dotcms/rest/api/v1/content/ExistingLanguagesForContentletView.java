package com.dotcms.rest.api.v1.content;

import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This View is used to indicate what language a
 * {@link com.dotmarketing.portlets.contentlet.model.Contentlet} is available on, and what language
 * it is not. This is particularly useful for the UI to be aware of such a situation, and then
 * provide the user the option to create it using that language.
 *
 * @author Jose Castro
 * @since Jan 5th, 2023
 */
public class ExistingLanguagesForContentletView extends HashMap<String, Object> implements Serializable {

    public ExistingLanguagesForContentletView(final Language language, final boolean translated) {
        final Map<String, Object> dataMap = new HashMap<>(language.toMap());
        this.putAll(dataMap);
        this.put("translated", translated);
    }

}
