package com.dotmarketing.portlets.languagesmanager.model;

import java.io.Serializable;

public class LanguageKey implements Serializable, Comparable<LanguageKey> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String languageCode;
	private String countryCode;
	private String key;
	private String value;
	
	public LanguageKey(String languageCode, String countryCode, String key, String value) {
		super();
		this.languageCode = languageCode;
		this.countryCode = countryCode;
		this.key = key;
		this.value = value;
	}
	
	public String getLanguageCode() {
		return languageCode;
	}
	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	public int compareTo(LanguageKey o) {
		
		return this.getKey().compareTo(o.getKey());
	}
	
	
}
