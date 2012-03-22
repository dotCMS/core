package com.dotmarketing.portlets.mailinglists.model;

import java.io.Serializable;
import java.util.Map;

import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

/** @author David Torres */
public class MailingList extends Inode implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String userId, title;
	private boolean publicList;

	public MailingList() {
		super.setType("mailing_list");
		publicList = false;
	};

	/**
	 * Returns the publicList.
	 * @return boolean
	 */
	public boolean isPublicList() {
		return publicList;
	}

	/**
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns the userId.
	 * @return String
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Sets the publicList.
	 * @param publicList The publicList to set
	 */
	public void setPublicList(boolean publicList) {
		this.publicList = publicList;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Sets the userId.
	 * @param userId The userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public Map<String, Object> getMap() throws DotStateException, DotDataException, DotSecurityException {
		Map<String, Object> map = super.getMap();
		map.put("title", title);
		map.put("userId", userId);
		map.put("publicList", publicList);
		return map;
	}
	
	

}
