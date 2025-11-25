/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.achecker.model;

import com.dotcms.enterprise.achecker.utility.Constants;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * This class represents a Validation Guideline use by the Accessibility Checker to determine
 * whether a specified content meets the expected requirements. Content Authors can select the
 * guideline they want to validate against a given content.
 *
 * @author root
 * @since N/A
 */
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

	@Override
	public String toString() {
		return "GuideLineBean{" +
				"preamble='" + preamble + '\'' +
				", earlid='" + earlid + '\'' +
				", long_name='" + long_name + '\'' +
				", abbr='" + abbr + '\'' +
				", title='" + title + '\'' +
				", guideline_id=" + guideline_id +
				", user_id=" + user_id +
				", status=" + status +
				", open_to_public=" + open_to_public +
				", seal_icon_name='" + seal_icon_name + '\'' +
				", subset='" + subset + '\'' +
				", defaultGuideLine=" + defaultGuideLine +
				'}';
	}

}
