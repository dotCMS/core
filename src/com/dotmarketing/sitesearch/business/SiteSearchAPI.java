package com.dotmarketing.sitesearch.business;


public interface SiteSearchAPI {
	
	
	public DotSearchResults search(String query, String sort, int start, int rows, String lang, String hostId); 

}
