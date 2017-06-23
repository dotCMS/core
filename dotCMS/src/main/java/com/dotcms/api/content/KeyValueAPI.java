package com.dotcms.api.content;

import java.util.List;

import com.dotcms.content.model.KeyValue;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

/**
 * Provides access to Key/Value contents in the system.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 *
 */
public interface KeyValueAPI {

    public KeyValue fromContentlet(final Contentlet contentlet);

    public List<KeyValue> get(final String key, final User user, final boolean respectFrontEnd);

    public List<KeyValue> get(final String key, final long languageId, final User user, final boolean respectFrontEnd);
    
    public List<KeyValue> get(final String key, final ContentType contentType, final User user, final boolean respectFrontEnd);

    public KeyValue get(final String key, final long languageId, final ContentType contentType, final User user, final boolean respectFrontEnd);

}
