package com.dotmarketing.sitesearch.business;

import com.dotcms.content.elasticsearch.business.DotIndexException;
import com.dotcms.content.elasticsearch.business.ESIndexAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;


/**
 * 
 * @author Roger
 *
 */
public class SiteSearchAPIImpl implements SiteSearchAPI{
	
	private ESIndexAPI esapi = APILocator.getESIndexAPI();

	
	public void createSiteSearchIndex(String indexName) throws DotIndexException{
		
		

		try {
			esapi.createIndex(indexName);
		} catch (Exception e) {
			Logger.error(SiteSearchAPIImpl.class,e.getMessage(),e);
			throw new DotIndexException(e.getMessage());
		} 
		
		
		
		return;
		
	}
	
	
	
	
	
	
	
	public DotSearchResults search(String query, String sort, int start, int rows, String lang, String hostId) {
		
        DotHits hits = null;
		try {
			hits = getHits(query,sort,start,rows,lang,hostId);
		} catch (Exception e) {
			Logger.error(this, e.getMessage(), e);
			return null;
		}
		DotSearchResults results = new DotSearchResults();
		results.setQuery(query);
		results.setLang(lang);
		results.setSort(sort);
		results.setReverse(false);
		results.setStart(start);
		results.setRows(rows);
		results.setEnd(hits.getLength());
		results.setTotalHits(hits.getTotalHits());
		results.setHits(hits.getHits());
		results.setDetails(hits.getDetails());
		results.setSummaries(hits.getSummary());
		results.setWithSummary(true);
		return results;
	}
	
	/**
	 * 
	 * @param query
	 * @param sort
	 * @param start
	 * @param rows
	 * @param lang
	 * @param hostId
	 * @return
	 * @throws Exception
	 */
	private DotHits getHits(String query, String sort, int start, int rows, String lang, String hostId) throws Exception{
		return null;

	}
}
