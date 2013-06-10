package com.dotcms.publisher.environment.bean;

import java.io.Serializable;

public class Environment implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = -8154524665918330146L;

	private String id;
	private String name;
	private Boolean pushToAll;

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Boolean getPushToAll() {
		return pushToAll;
	}
	public void setPushToAll(Boolean pushToAll) {
		this.pushToAll = pushToAll;
	}

}
