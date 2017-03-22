package com.dotcms.contenttype.transform.field;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.business.DotStateException;

public class DbFieldVariableTransformer {

	final List<Map<String, Object>> results;

	public DbFieldVariableTransformer(Map<String, Object> map) {
		this.results = ImmutableList.of(map);
	}

	public DbFieldVariableTransformer(List<Map<String, Object>> results) {
		this.results = results;
	}


	public FieldVariable from() throws DotStateException {
		if(this.results.size()==0) throw new DotStateException("0 results");
		return fromMap(results.get(0));

	}

	private static FieldVariable fromMap(Map<String, Object> map) {
		
		FieldVariable var = ImmutableFieldVariable.builder()
				.id((String) map.get("id"))
				.fieldId((String) map.get("field_id"))
				.name((String) map.get("variable_name"))		
				.key((String) map.get("variable_key"))			
				.value((String) map.get("variable_value"))		
				.userId((String) map.get("user_id"))
				.modDate((Date) map.get("last_mod_date"))
				.build();
		
		return var;

	}


	public List<FieldVariable> asList() throws DotStateException {
		List<FieldVariable> list = new ArrayList<FieldVariable>();
		for (Map<String, Object> map : results) {
			list.add(fromMap(map));
		}

		return ImmutableList.copyOf(list);
	}
	
	
	

}