package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;

public interface RelationshipFactory {

	void deleteByContentType(ContentType type) throws DotDataException;

}
