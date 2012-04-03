package com.dotmarketing.sitesearch.business;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.ElasticSearchException;

import com.dotmarketing.exception.DotDataException;


public interface SiteSearchAPI {
    public static final String ES_SITE_SEARCH_NAME = "sitesearch";
	
	public DotSearchResults search(String query, String sort, int start, int rows, String lang, String hostId);

	List<String> listIndices();

	void activateIndex(String indexName) throws DotDataException;

	void deactivateIndex(String indexName) throws DotDataException, IOException;

	boolean createSiteSearchIndex(String indexName, int shards) throws ElasticSearchException, IOException; 

}
