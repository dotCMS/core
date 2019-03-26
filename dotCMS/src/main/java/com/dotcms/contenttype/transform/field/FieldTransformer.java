package com.dotcms.contenttype.transform.field;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.DotStateException;
import java.util.List;

public interface FieldTransformer {

  Field from() throws DotStateException;

  List<Field> asList() throws DotStateException;
}
