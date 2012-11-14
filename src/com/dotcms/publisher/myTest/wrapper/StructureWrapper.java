package com.dotcms.publisher.myTest.wrapper;

import java.util.List;

import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;

public class StructureWrapper {
	private Structure structure;
	private List<Field> fields;
	
	public StructureWrapper(Structure structure, List<Field> fields) {
		this.structure = structure;
		this.fields = fields;
	}

	public Structure getStructure() {
		return structure;
	}

	public void setStructure(Structure structure) {
		this.structure = structure;
	}

	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}
}
