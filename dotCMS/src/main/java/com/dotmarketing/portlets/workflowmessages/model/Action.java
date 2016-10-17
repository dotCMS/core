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
public class Action implements Serializable, Comparable {
	
	private static final long serialVersionUID = 1L;

	private long id;
	
	private String title;
	
	private long antiStatusId;
	
	private long postStatusId;

	private int permission;
	
	private boolean emailAllWithPermissions;
	

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
		if(!(compObject instanceof Action))return -1;
		
		Action action = (Action) compObject;
		return (action.getTitle().compareTo(this.getTitle()));
	}

	/**
	 * @return
	 */
	public long getAntiStatusId() {
		return antiStatusId;
	}

	/**
	 * @return
	 */
	public long getPostStatusId() {
		return postStatusId;
	}

	/**
	 * @param l
	 */
	public void setAntiStatusId(long statusId) {
		antiStatusId = statusId;
	}

	/**
	 * @param l
	 */
	public void setPostStatusId(long statusId) {
		postStatusId = statusId;
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
	public void setPermission(int i) {
		permission = i;
	}

	/**
	 * @return
	 */
	public boolean isEmailAllWithPermissions() {
		return emailAllWithPermissions;
	}

	/**
	 * @param b
	 */
	public void setEmailAllWithPermissions(boolean b) {
		emailAllWithPermissions = b;
	}

}
