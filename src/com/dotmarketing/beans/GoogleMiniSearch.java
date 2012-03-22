package com.dotmarketing.beans;

import java.util.HashMap;
import java.util.List;

import com.dotmarketing.util.UtilMethods;

public class GoogleMiniSearch {
	private String searchTime;
	private String query;
	private HashMap<String, HashMap<String, String>> params;
	private String contextTitle;
	private int fromIndex;
	private int toIndex;
	private int estimatedTotal;
	private String previousResultPageRelativeURL;
	private String nextResultPageRelativeURL;
	private List<GoogleMiniSearchResult> searchResults;
	private List<GoogleMiniSearchResult> keyMatchResults;
	
	

	public List<GoogleMiniSearchResult> getKeyMatchResults() {
		return keyMatchResults;
	}

	public void setKeyMatchResults(List<GoogleMiniSearchResult> keyMatchResults) {
		this.keyMatchResults = keyMatchResults;
	}
	
	public int getEstimatedTotal() {
		return estimatedTotal;
	}
	
	public void setEstimatedTotal(int estimatedTotal) {
		this.estimatedTotal = estimatedTotal;
	}
	
	public int getFromIndex() {
		return fromIndex;
	}
	
	public void setFromIndex(int fromIndex) {
		this.fromIndex = fromIndex;
	}
	
	public void setNextResultPageRelativeURL(String nextResultPageRelativeURL) {
		this.nextResultPageRelativeURL = nextResultPageRelativeURL;
	}
	
	public void setPreviousResultPageRelativeURL(
			String previousResultPageRelativeURL) {
		this.previousResultPageRelativeURL = previousResultPageRelativeURL;
	}
	
	public String getQuery() {
		return query;
	}
	
	public void setQuery(String query) {
		this.query = query;
	}
	
	public List<GoogleMiniSearchResult> getSearchResults() {
		return searchResults;
	}
	
	public void setSearchResults(List<GoogleMiniSearchResult> searchResults) {
		this.searchResults = searchResults;
	}
	
	public String getSearchTime() {
		return searchTime;
	}
	
	public void setSearchTime(String searchTime) {
		this.searchTime = searchTime;
	}
	
	public int getToIndex() {
		return toIndex;
	}
	
	public void setToIndex(int toIndex) {
		this.toIndex = toIndex;
	}

	public boolean hasNextPage() {
		if(UtilMethods.isSet(nextResultPageRelativeURL))
			return true;
		return false;
	}
	
	public boolean hasPreviousPage() {
		if(UtilMethods.isSet(previousResultPageRelativeURL))
			return true;
		return false;
	}
	
	@Override
	public String toString() {
		StringBuffer st = new StringBuffer("GoogleMiniSearch [\n");
		st.append("searchTime = " + searchTime + "\n");
		st.append("query = " + query + "\n");
		st.append("fromIndex = " + fromIndex + "\n");
		st.append("toIndex = " + toIndex + "\n");
		st.append("estimatedTotal = " + estimatedTotal + "\n");
		st.append("previousResultPageRelativeURL = " + previousResultPageRelativeURL + "\n");
		st.append("nextResultPageRelativeURL = " + nextResultPageRelativeURL + "]");
		return st.toString();
	}

	public HashMap<String, HashMap<String, String>> getParams() {
		return params;
	}

	public void setParams(HashMap<String, HashMap<String, String>> params) {
		this.params = params;
	}

	public String getContextTitle() {
		return contextTitle;
	}

	public void setContextTitle(String contextTitle) {
		this.contextTitle = contextTitle;
	}
}