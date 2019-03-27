package com.dotmarketing.portlets.dashboard.model;

public enum ViewType {
	
	DAY("day"),
	WEEK("week"),
	MONTH("month");
	
	private String value;
	
	ViewType (String value) {
		this.value = value;
	}
	
	public String toString () {
		return value;
	}
		
	public static ViewType getViewType(String value) {
		ViewType[] ojs = ViewType.values();
		for (ViewType oj : ojs) {
			if (oj.value.equalsIgnoreCase(value))
				return oj;
		}
		return null;
	}

}
