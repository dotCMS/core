package com.dotcms.contenttype.transform.contenttype;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.DotStateException;
import java.util.List;

public interface ContentTypeTransformer {

  ContentType from() throws DotStateException;

  List<ContentType> asList() throws DotStateException;
}
