package com.dotcms.achecker.model;


import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import com.dotcms.achecker.utility.Constants;
import com.dotcms.achecker.model.ReflectionBean;

public class GuideLineBean extends ReflectionBean {
 
			
	private String preamble;
	private String earlid;
	private String long_name;
	private String abbr;
	private String title;
	private int guideline_id;
	private int user_id;
	private int status;
	private int open_to_public;
	private String seal_icon_name;
	private String subset;
	private boolean defaultGuideLine;	

	public GuideLineBean(Map<String, Object> init)
			throws IntrospectionException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		super(init);
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getPreamble() {
		return preamble;
	}

	public void setPreamble(String preamble) {
		this.preamble = preamble;
	}

	public String getEarlid() {
		return earlid;
	}

	public void setEarlid(String earlid) {
		this.earlid = earlid;
	}

	public String getLong_name() {
		return long_name;
	}

	public void setLong_name(String longName) {
		long_name = longName;
	}

	public String getAbbr() {
		return abbr;
	}

	public void setAbbr(String abbr) {
		this.abbr = abbr;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getGuideline_id() {
		return guideline_id;
	}

	public void setGuideline_id(int guidelineId) {
		guideline_id = guidelineId;
	}

	public int getUser_id() {
		return user_id;
	}

	public void setUser_id(int userId) {
		user_id = userId;
	}

	public int getOpen_to_public() {
		return open_to_public;
	}

	public void setOpen_to_public(int openToPublic) {
		open_to_public = openToPublic;
	}

	public String getSeal_icon_name() {
		return seal_icon_name;
	}

	public void setSeal_icon_name(String sealIconName) {
		seal_icon_name = sealIconName;
	}

	public String getSubset() {
		return subset;
	}

	public void setSubset(String subset) {
		this.subset = subset;
	}
	
	public boolean isDefaultGuideLine() {
		return Constants.DEFAULT_GUIDELINE.equalsIgnoreCase(this.abbr);
	}

	 
}
