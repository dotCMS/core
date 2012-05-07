package com.dotcms.achecker.parsing;

public class DoctypeBean {
	
	private String name;
	
	private String publicId;
	
	private String systemId;
	
	public DoctypeBean(String name, String publicId, String systemId) {
		super();
		this.name = name;
		this.publicId = publicId;
		this.systemId = systemId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPublicId() {
		return publicId;
	}

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}

	public String getSystemId() {
		return systemId;
	}

	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}

	
}
