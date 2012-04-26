package com.dotmarketing.sitesearch.business;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.elasticsearch.ElasticSearchException;
import org.quartz.SchedulerException;

import com.dotcms.publishing.sitesearch.DotSearchResults;
import com.dotcms.publishing.sitesearch.SiteSearchConfig;
import com.dotcms.publishing.sitesearch.SiteSearchResult;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.quartz.ScheduledTask;


public interface SiteSearchAPI {
    public static final String ES_SITE_SEARCH_NAME = "sitesearch";
    public static final String ES_SITE_SEARCH_MAPPING = "dot_site_search";
    

	List<String> listIndices();

	void activateIndex(String indexName) throws DotDataException;

	void deactivateIndex(String indexName) throws DotDataException, IOException;

	boolean createSiteSearchIndex(String indexName, int shards) throws ElasticSearchException, IOException; 

	
	List<ScheduledTask> getTasks() throws SchedulerException;

	void deleteTask(String taskName) throws SchedulerException;

	void scheduleTask(SiteSearchConfig config) throws SchedulerException, ParseException, ClassNotFoundException;

	void putToIndex(String idx, SiteSearchResult res);

	void putToIndex(String idx, List<SiteSearchResult> res);

	void deleteFromIndex(String idx, String docId);

	DotSearchResults search(String query, String sort, int start, int rows);
	
	DotSearchResults search(String indexName, String query, String sort, int start, int rows);

	ScheduledTask getTask(String taskName) throws SchedulerException;
	
	void pauseTask(String taskName)  throws SchedulerException;

	int getTaskProgress(String jobName) throws SchedulerException;
}
