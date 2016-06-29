package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;

public interface ContentTypeFactory {
	ContentType find(String id) throws DotDataException;
	
	ContentType findByVar(String id);
	
}
