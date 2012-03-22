package com.dotmarketing.db;

import net.sf.hibernate.cfg.DefaultNamingStrategy;

public class LowercaseNamingStrategy extends DefaultNamingStrategy {
	
	public LowercaseNamingStrategy() {
		super();	
	}
	
	
	public String classToTableName(String className) {
		return tableName(className);
	}
	
	
	public String columnName(String columnName) {
		return columnName.toLowerCase();
	}
	

	public String propertyToColumnName(String propertyName) {
		return columnName(propertyName);
	}
	
	public String tableName(String tableName) {
		return tableName.toLowerCase();
	}
	
	public String propertyToTableName(String className, String propertyName){
		return className.toLowerCase()+"."+propertyName.toLowerCase();
	}
	
}
	


