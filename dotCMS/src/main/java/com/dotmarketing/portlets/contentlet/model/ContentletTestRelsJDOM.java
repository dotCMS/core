package com.dotmarketing.portlets.contentlet.model;

import java.util.List;

public class ContentletTestRelsJDOM {
	String name;
	List<String> keyFields;
	List<String> fieldValues;
	
	public ContentletTestRelsJDOM() {}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<String> getKeyFields() {
		return keyFields;
	}
	public void setKeyFields(List<String> keyFields) {
		this.keyFields = keyFields;
	}
	public List<String> getFieldValues() {
		return fieldValues;
	}
	public void setFieldValues(List<String> fieldValues) {
		this.fieldValues = fieldValues;
	}
}
