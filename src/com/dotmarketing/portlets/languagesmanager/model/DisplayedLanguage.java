package com.dotmarketing.portlets.languagesmanager.model;

public class DisplayedLanguage {
	private Language language;
	private boolean greyedOut;
	
	public DisplayedLanguage(Language language, boolean greyedOut) {
		this.language = language;
		this.greyedOut = greyedOut;
	}
	
	public Language getLanguage() {
		return language;
	}
	public void setLanguage(Language language) {
		this.language = language;
	}
	public boolean isGreyedOut() {
		return greyedOut;
	}
	public void setGreyedOut(boolean greyedOut) {
		this.greyedOut = greyedOut;
	}
	
}
