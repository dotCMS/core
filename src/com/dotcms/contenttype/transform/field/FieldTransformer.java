package com.dotcms.contenttype.transform.field;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.DotStateException;

public interface FieldTransformer {

	Field from() throws DotStateException;

	List<Field> asList() throws DotStateException;

}
