package com.dotmarketing.portlets.chains.model;

import java.util.Date;

/**
 * 
 * @author davidtorresv
 *
 */
public class ChainLinkCode {
	
	public enum Language {
		
		JAVA("java"),
		GROOVY("groovy"),
		BEANSHELL("beanshell"),
		UNKNOWN("unknown");
		
		String key;
		
		Language(String key) {
			this.key = key;
		}

		@Override
		public String toString() {
			return key.toString();
		}
		
		public static Language getLanguage(String key) {
			if (JAVA.toString().equals(key))
				return JAVA;
			else if(GROOVY.toString().equals(key))
				return GROOVY;
			else if(BEANSHELL.toString().equals(key))
				return BEANSHELL;
			else 
				return UNKNOWN;		
				
		}
	
	}
	
	private long id;

	private String className;
	
	private String code;
	
	private Date lastModifiedDate = new Date();
	
	private String language;
	
	public ChainLinkCode () {
		
	}
	
	public ChainLinkCode(String className, String code) {
		this.className = className;
		this.code = code;
	}
	
	public ChainLinkCode(String className, String code, Language lang) {
		this(className, code);
		this.language = lang.key;
	}
	
	public ChainLinkCode(String className, String code, Language lang, Date lastModified) {
		this(className, code, lang);
		this.lastModifiedDate= lastModified;		
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public void setClassName(String linkCodeClassName) {
		this.className = linkCodeClassName;
	}
	public String getClassName() {
		return className;
	}
	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}
	public void setLanguage(Language language) {
		this.language = language.toString();
	}
	public void setLanguageKey(String languageKey) {
		this.language = Language.getLanguage(languageKey).toString();
	}
	public Language getLanguage() {
		return Language.getLanguage(this.language);
	}
	public String getLanguageKey() {
		return Language.getLanguage(this.language).toString();
	}	
	
}
