package com.dotcms.publishing.sitesearch;

import java.util.ArrayList;
import java.util.List;

public class SiteSearchResults {

	float maxScore=0;
	int limit=50;
	int offset=0;
	String query;
	long totalResults=0;
	List<SiteSearchResult> results = new ArrayList<SiteSearchResult>();
	String index;
	String error=null;
	String took;
	/**
	 * Backward compatable alias
	 * for getOffset()
	 * @return
	 */
	public int getStart(){
		return this.getOffset();
	}
	/**
	 * Backward compatible alias
	 * for getTotalResults()
	 * @return
	 */
	public long getTotalHits(){
		return this.getTotalResults();
	}
	/**
	 * How long the query took
	 * @return
	 */
	public String getTook() {
		return took;
	}
	public void setTook(String took) {
		this.took = took;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public long getTotalResults() {
		return totalResults;
	}
	public void setTotalResults(long totalResults) {
		this.totalResults = totalResults;
	}
	public List<SiteSearchResult> getResults() {
		return results;
	}
	public void setResults(List<SiteSearchResult> results) {
		this.results = results;
	}
	public String getIndex() {
		return index;
	}
	public void setIndex(String index) {
		this.index = index;
	}
	public float getMaxScore() {
		return maxScore;
	}
	public void setMaxScore(float maxScore) {
		this.maxScore = maxScore;
	}


	
	
}
