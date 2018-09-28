package com.dotcms.contenttype.transform.relationship;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.portlets.structure.model.Relationship;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DbRelationshipTransformer implements RelationshipTransformer{
	final List<Relationship> list;


	public DbRelationshipTransformer(List<Map<String, Object>> initList){
		List<Relationship> newList = new ArrayList<>();
		if (initList != null){
			for(Map<String, Object> map : initList){
				newList.add(fromMap(map));
			}
		}

		this.list = newList;
	}

	@Override
	public List<Relationship> asList() {
		return ImmutableList.copyOf(list);
	}

	private static Relationship fromMap(Map<String, Object> map) {

		Relationship var = new Relationship();
		var.setInode((String) map.get("inode"));
		var.setParentStructureInode((String) map.get("parent_structure_inode"));
		var.setChildStructureInode((String) map.get("child_structure_inode"));
		var.setParentRelationName((String) map.get("parent_relation_name"));
		var.setChildRelationName((String) map.get("child_relation_name"));
		var.setRelationTypeValue((String) map.get("relation_type_value"));
		var.setCardinality((Integer) map.get("cardinality"));

		return var;

	}
}