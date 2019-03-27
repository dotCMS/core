package com.dotmarketing.portlets.workflows.model;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

public class WorkflowSearcher {

	String schemeId;
	String assignedTo;
	String createdBy;
	String stepId;
	boolean open;
	boolean closed;
	boolean show4all;
	String keywords;
	String orderBy;
	int count = 20;
	int page = 0;
	User user;
	int totalCount;
	int daysOld=-1;
	
	public int getDaysOld() {
	    return daysOld;
	}

	public boolean getShow4All() {
	    return show4all;
	}
	
	public void setShow4All(boolean value) {
	    show4all=value;
	}
	
	public int getTotalCount() {
		return totalCount;
	}

	public String getAssignedTo() {
		return assignedTo;
	}

	public void setAssignedTo(String assignedTo) {
		this.assignedTo = assignedTo;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	private String getStringValue(String x, Map<String, Object> map) {
		Object obj = map.get(x);
		if (obj == null)
			return null;
		String ret = null;
		if (obj instanceof String[]) {
			ret = ((String[]) obj)[0];
		} else {
			ret = obj.toString();
		}
		return (UtilMethods.isSet(ret)) ? ret : null;

	}

	private boolean getBooleanValue(String x, Map<String, Object> map) {
		Object obj = map.get(x);
		String y = null;
		try {

			if (obj instanceof String[]) {
				y = ((String[]) obj)[0];
			} else {
				y = obj.toString();
			}
			return new Boolean(y);
		} catch (Exception e) {

		}
		return false;
	}

	public WorkflowSearcher() {
	}

	public WorkflowSearcher(Map<String, Object> map, User user) {

		schemeId = getStringValue("schemeId", map);
		assignedTo = getStringValue("assignedTo", map);
		createdBy = getStringValue("createdBy", map);
		stepId = getStringValue("stepId", map);
		keywords = getStringValue("keywords", map);
		orderBy = getStringValue("orderBy", map);
		
		
		orderBy= SQLUtil.sanitizeSortBy(orderBy);
		show4all = getBooleanValue("show4all", map);
		open = getBooleanValue("open", map);
		closed = getBooleanValue("closed", map);
		String daysOldstr = getStringValue("daysold", map);
		
		if(UtilMethods.isSet(daysOldstr) && daysOldstr.matches("^\\d+$"))
		    daysOld = Integer.parseInt(daysOldstr);

		this.user = user;

		try {
			page = Integer.parseInt((String) getStringValue("page", map));
		} catch (Exception e) {
			page = 0;
		}
		if (page < 0)
			page = 0;
		try {
			count = Integer.parseInt((String) getStringValue("count", map));
		} catch (Exception e) {
			count = 20;
		}
		if (count < 0)
			count = 20;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getOrderBy() {
		return SQLUtil.sanitizeSortBy(orderBy);
	}

	public List<WorkflowTask> findTasks() throws DotDataException {

		totalCount = APILocator.getWorkflowAPI().countTasks(this);
		return APILocator.getWorkflowAPI().searchTasks(this);

	}

	public List<WorkflowTask> findAllTasks(WorkflowSearcher searcher) throws DotDataException {

		List<WorkflowTask> list = APILocator.getWorkflowAPI().searchAllTasks(searcher);
		totalCount = (APILocator.getWorkflowAPI().searchAllTasks(null)).size();
		return list;

	}

	public void setOrderBy(String orderBy) {
		orderBy = SQLUtil.sanitizeSortBy(orderBy);
		this.orderBy = orderBy;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public String getSchemeId() {
		return schemeId;
	}

	public void setSchemeId(String schemeId) {
		this.schemeId = schemeId;
	}

	public String getStepId() {
		return stepId;
	}

	public void setStepId(String actionId) {
		this.stepId = actionId;
	}

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getQueryString() {
		return   "&schemeId=" + UtilMethods.webifyString(schemeId) 
		        + "&assignedTo=" + UtilMethods.webifyString(assignedTo) 
		        + "&createdBy=" + UtilMethods.webifyString(createdBy)
				+ "&stepId=" + UtilMethods.webifyString(stepId) 
				+ "&open=" + open 
				+ "&closed=" + closed 
				+ "&keywords=" + URLEncoder.encode(UtilMethods.webifyString(keywords))
				+ "&orderBy=" + orderBy 
				+ "&count=" + count
				+ ((show4all) ? "&show4all=true" : "")
				+ "&page=" + page;

	}

	public String getQueryStringBis() {
		return "&count=" + count + "&page=" + page;

	}

	public boolean hasBack() {
		return page > 0;

	}

	public boolean hasNext() {
		return (count * (page + 1)) < totalCount;

	}

	public int getTotalPages() {

		return ((totalCount - 1) / count) + 1;
	}

	public int getStartPage() {
		int startPages = 0;
		int pages = getTotalPages();
		if (pages > 10 && page > pages - 10) {
			startPages = pages - 10;
		}
		return startPages;
	}

}
