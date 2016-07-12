package com.dotcms.contenttype.transform.contenttype;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.DotStateException;

public interface ToContentTypeTransformer {

	ContentType from() throws DotStateException;

	List<ContentType> asList() throws DotStateException;

}
