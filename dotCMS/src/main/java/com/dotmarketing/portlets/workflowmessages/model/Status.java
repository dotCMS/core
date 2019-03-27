/*
 * Created on Apr 5, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.dotmarketing.portlets.workflowmessages.model;

import java.io.Serializable;

/**
 * @author rocco
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Status implements Serializable, Comparable {
	
	private static final long serialVersionUID = 1L;

	private long id;
	
	private String title;
		
	private int permission;

	/**
	 * @return
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param l
	 */
	public void setId(long l) {
		id = l;
	}

	/**
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param string
	 */
	public void setTitle(String string) {
		title = string;
	}

	public int compareTo(Object compObject){
		if(!(compObject instanceof Status))return -1;
		Status status = (Status) compObject;
		return (status.getTitle().compareTo(this.getTitle()));
	}

	/**
	 * @return
	 */
	public int getPermission() {
		return permission;
	}

	/**
	 * @param i
	 */
	public void setPermission(int permission) {
		this.permission = permission;
	}

}
