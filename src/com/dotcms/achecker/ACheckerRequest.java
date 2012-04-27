package com.dotcms.achecker;

public class ACheckerRequest {
	
	private String lang;
	
	private String content;
	
	private String guide;
	
	private boolean fragment;
	
	public ACheckerRequest() {
	}
	
	public ACheckerRequest(String lang, String content, String guide, boolean fragment) {
		super();
		this.lang = lang;
		this.content = content;
		this.guide = guide;
		this.fragment = fragment;
	}
	
	public boolean isFragment() {
		return fragment;
	}

	public void setFragment(boolean fragment) {
		this.fragment = fragment;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getGuide() {
		return guide;
	}

	public void setGuide(String guide) {
		this.guide = guide;
	}

}
