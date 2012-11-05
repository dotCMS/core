package com.dotcms.publisher.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class CommonRowMapper<T> implements RowMapper<T> {
	
	public List<T> mapRows(List<Map<String, Object>> listMap) {
		List<T> listToReturn  = new ArrayList<T>();
		for (Map<String, Object> row : listMap) {
			listToReturn.add(mapObject(row));
		}		
		
		return listToReturn;
	}
	
	protected Integer getIntegerFromObj(Object obj) {
		try {
			return Integer.parseInt(obj.toString());
		} catch (Exception e) {
			return null;
		}
	}
}
