package com.dotcms.contenttype.transform.contenttype;

import java.util.List;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.structure.model.Structure;

public interface StructureTransformerIf extends ContentTypeTransformer {

	Structure asStructure() throws DotStateException;
	List<Structure> asStructureList() throws DotStateException;

}
