package com.dotcms.contenttype.business;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;

public interface FieldFactory {

	Field find(String id) throws DotDataException;

	ContentType findByVar(String id);

}
