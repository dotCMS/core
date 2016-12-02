package com.dotcms.contenttype.model.relationship;

import java.io.Serializable;
import java.util.List;

import org.immutables.value.Value;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

@Value.Immutable
public abstract class ContentletRelationshipRecords implements Serializable {


	private static final long serialVersionUID = 1L;
	Relationship relationship;
	List<Contentlet> records;
	boolean isHasParent;

	public void reorderRecords(String field) {
		/*
		 * String fieldContentletName = null;
		 * 
		 * Structure st = contentlet.getStructure(); List<Field> fields =
		 * st.getFields(); for (Field f : fields) { if
		 * (f.getFieldName().equals(field)) { fieldContentletName =
		 * f.getFieldContentlet(); break; } }
		 * 
		 * if (fieldContentletName != null) Collections.sort(records, new
		 * ContentComparator(fieldContentletName));
		 */

	}

}
