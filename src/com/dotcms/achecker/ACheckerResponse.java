package com.dotcms.achecker;


import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ACheckerResponse {
	
	private String lang;
	
	private List<AccessibilityResult> results;

	private List<AccessibilityResult> errors;
	
	public ACheckerResponse() {}

	public ACheckerResponse(List<AccessibilityResult> results, List<AccessibilityResult> errors, String lang) {
		super();
		this.errors = errors;
		this.results = results;
		this.lang = lang;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public List<AccessibilityResult> getResults() {
		return results;
	}

	public void setResults(List<AccessibilityResult> results) {
		this.results = results;
	}

	public List<AccessibilityResult> getErrors() {
		return errors;
	}

	public void setErrors(List<AccessibilityResult> errors) {
		this.errors = errors;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("success: {\n");
		for (AccessibilityResult result : getResults()) {
			if ( result.isSuccess() ) {
				buffer.append("\t" + result + "\n");
			}
		}
		buffer.append("}\n");
		buffer.append("failed: {\n");
		for (AccessibilityResult result : getResults()) {
			if ( !result.isSuccess() ) {
				buffer.append("\t" + result + "\n");
			}
		}
		buffer.append("}\n");
		return buffer.toString();
	}

}
