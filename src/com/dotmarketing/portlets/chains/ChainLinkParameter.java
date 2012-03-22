package com.dotmarketing.portlets.chains;

/**
 * 
 * This is a meta-data class that need to be returned by link implementations
 * to tell what parameter the link receives/requires and how to use the parameters
 * 
 * @author davidtorresv
 *
 */
public final class ChainLinkParameter {

	private String name;
	private String description;
	private boolean required;
	
	public ChainLinkParameter (String name, boolean required, String description) {
		this.name = name;
		this.description = description;
		this.required = required;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}
	
	
	
}
