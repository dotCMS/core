package com.dotcms.keyvalue.model;

import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 20, 2017
 *
 */
public class DefaultKeyValue extends Contentlet implements KeyValue {

	private static final long serialVersionUID = 1L;

	@Override
	public String getKey() {
		return getStringProperty(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR);
	}

	@Override
	public void setKey(String key) {
		setStringProperty(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR, key);
	}

	@Override
	public String getValue() {
		return getStringProperty(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR);
	}

	@Override
	public void setValue(String value) {
		setStringProperty(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR, value);
	}
	
}
