package com.dotcms.contenttype.transform.field;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.business.DotStateException;

public interface LegacyFieldTransformerIf extends FieldTransformer {

	com.dotmarketing.portlets.structure.model.Field asOldField() throws DotStateException;

	List<com.dotmarketing.portlets.structure.model.Field> asOldFieldList() throws DotStateException;

}
