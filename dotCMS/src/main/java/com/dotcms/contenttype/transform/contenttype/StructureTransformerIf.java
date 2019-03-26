package com.dotcms.contenttype.transform.contenttype;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.structure.model.Structure;
import java.util.List;

public interface StructureTransformerIf extends ContentTypeTransformer {

  Structure asStructure() throws DotStateException;

  List<Structure> asStructureList() throws DotStateException;
}
