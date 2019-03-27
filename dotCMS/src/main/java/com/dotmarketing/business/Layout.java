package com.dotmarketing.business;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Layout implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2604223225988451297L;
	private String id = "";
	private String name;
	private String description;
	private int tabOrder;
	private List<String> portletIds;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getTabOrder() {
		return tabOrder;
	}
	public void setTabOrder(int tabOrder) {
		this.tabOrder = tabOrder;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<String> getPortletIds() {
		return portletIds;
	}
	public void setPortletIds(List<String> portletIds) {
		this.portletIds = portletIds;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> layoutMap = new HashMap<String, Object>();
		layoutMap.put("id", id);
		layoutMap.put("name", name);
		layoutMap.put("description", description);
		layoutMap.put("tabOrder", tabOrder);
		layoutMap.put("portletIds", portletIds);
		return layoutMap;
	}
}
