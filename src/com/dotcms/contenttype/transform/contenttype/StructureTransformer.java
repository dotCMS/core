package com.dotcms.contenttype.transform.contenttype;

import java.util.List;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.structure.model.Structure;

public interface StructureTransformer {

	Structure from() throws DotStateException;

	List<Structure> asList() throws DotStateException;

}
