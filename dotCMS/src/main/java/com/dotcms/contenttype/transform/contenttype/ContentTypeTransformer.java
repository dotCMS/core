package com.dotcms.contenttype.transform.contenttype;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.DotStateException;

public interface ContentTypeTransformer {

	ContentType from() throws DotStateException;

	List<ContentType> asList() throws DotStateException;

}
