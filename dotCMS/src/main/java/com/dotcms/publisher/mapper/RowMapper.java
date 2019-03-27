package com.dotcms.publisher.mapper;

import java.util.Map;

/**
 * Maps Map to a Java Object
 * @author alberto
 *
 */
public interface RowMapper<T> {
	/**
	 * Map the row
	 * @param row
	 * @return
	 */
	public T mapObject(Map<String, Object> row);
}
