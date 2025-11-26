/*
*
* Copyright (c) 2025 dotCMS LLC
* Use of this software is governed by the Business Source License included
* in the LICENSE file found at in the root directory of software.
* SPDX-License-Identifier: BUSL-1.1
*
*/

/**
 * 
 */
package com.dotcms.enterprise.cluster.action.model;

import java.util.Date;

import com.dotmarketing.util.json.JSONObject;

/**
 * Wrapper class for cluster_server_action of the Database.
 * 
 * @author Oscar Arrieta
 *
 */
public class ServerActionBean {
	
	private String id;
	private String originatorId;
	private String serverId;
	private boolean failed;
	private JSONObject response;
	private String serverActionId;
	private boolean completed;
	private Date enteredDate;
	private Long timeOutSeconds;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOriginatorId() {
		return originatorId;
	}
	public void setOriginatorId(String originatorId) {
		this.originatorId = originatorId;
	}
	public String getServerId() {
		return serverId;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public boolean isFailed() {
		return failed;
	}
	public void setFailed(boolean failed) {
		this.failed = failed;
	}
	public JSONObject getResponse() {
		return response;
	}
	public void setResponse(JSONObject response) {
		this.response = response;
	}
	public String getServerActionId() {
		return serverActionId;
	}
	public void setServerActionId(String serverActionId) {
		this.serverActionId = serverActionId;
	}
	public boolean isCompleted() {
		return completed;
	}
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	public Date getEnteredDate() {
		return enteredDate;
	}
	public void setEnteredDate(Date enteredDate) {
		this.enteredDate = enteredDate;
	}
	public Long getTimeOutSeconds() {
		return timeOutSeconds;
	}
	public void setTimeOutSeconds(Long timeOutSeconds) {
		this.timeOutSeconds = timeOutSeconds;
	}

}
