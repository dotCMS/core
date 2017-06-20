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

	public KeyValue fromContentlet(Contentlet contentlet);

	public List<KeyValue> get(String key, User user, boolean respectFrontEnd);

	public List<KeyValue> get(String key, ContentType contentType, User user, boolean respectFrontEnd);

	public KeyValue get(String key, long language, ContentType contentType, User user, boolean respectFrontEnd);

}
