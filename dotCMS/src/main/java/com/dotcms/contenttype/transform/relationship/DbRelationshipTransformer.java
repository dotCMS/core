package com.dotcms.contenttype.transform.relationship;

import com.google.common.collect.ImmutableList;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.portlets.structure.model.Relationship;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DbRelationshipTransformer implements RelationshipTransformer{

	final List<Map<String, Object>> results;

	public DbRelationshipTransformer(final List<Map<String, Object>> list){
		this.results = list;
	}

	public DbRelationshipTransformer(final Map<String, Object> map){
		this.results = ImmutableList.of(map);
	}

	@Override
	public List<Relationship> asList() {
		final List<Relationship> newList = new ArrayList<>();
		for(Map<String,Object> map : results){
			newList.add(fromMap(map));
		}
		return newList;
	}

	@Override
	public Relationship from(){
		return fromMap(results.get(0));
	}

	private static Relationship fromMap(final Map<String, Object> map) {

		final Relationship var = new Relationship();
		var.setInode((String) map.get("inode"));
		var.setParentStructureInode((String) map.get("parent_structure_inode"));
		var.setChildStructureInode((String) map.get("child_structure_inode"));
		var.setParentRelationName((String) map.get("parent_relation_name"));
		var.setChildRelationName((String) map.get("child_relation_name"));
		var.setRelationTypeValue((String) map.get("relation_type_value"));
		var.setFixed(DbConnectionFactory.isDBTrue(map.get("fixed").toString()));
		var.setParentRequired(DbConnectionFactory.isDBTrue(map.get("parent_required").toString()));
		var.setChildRequired(DbConnectionFactory.isDBTrue(map.get("child_required").toString()));
		var.setCardinality(ConversionUtils.toInt(map.get("cardinality"), 0));
		var.setModDate((Date) map.get("mod_date"));

		return var;

	}
}