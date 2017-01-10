package com.dotmarketing.portlets.structure.model;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FieldVariable    {

    private static final long serialVersionUID = 1L;
    
    private String id = "";
    private String fieldId = "";
    private String name = "" ;
    private String key = "" ;
    private String value = "" ;
    private String lastModifierId = "" ;
    private Date lastModDate;
    
	public String getFieldId() {
		return fieldId;
	}
	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
	}
	public String getName() {
		return name;
	}
	public void setName(String variableName) {
		this.name = variableName;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String variableKey) {
		this.key = variableKey;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String variableValue) {
		this.value = variableValue;
	}
	public String getLastModifierId() {
		return lastModifierId;
	}
	public void setLastModifierId(String userId) {
		this.lastModifierId = userId;
	}
	public Date getLastModDate() {
		return lastModDate;
	}
	public void setLastModDate(Date lastModDate) {
		this.lastModDate = lastModDate;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getId() {
		return id;
	}
	public Map<String, Object> getMap() {
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("id", id);
		returnMap.put("fieldId", fieldId);
		returnMap.put("name", name);
		returnMap.put("key", key);
		returnMap.put("value", value);
		returnMap.put("lastModifierId", lastModifierId);
		returnMap.put("lastModDate", lastModDate);
		return returnMap;
	}	

}
