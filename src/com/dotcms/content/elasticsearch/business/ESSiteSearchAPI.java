package com.dotcms.content.elasticsearch.business;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;

import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.business.DotSearchResults;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;

public class ESSiteSearchAPI implements SiteSearchAPI{

	private static final ESIndexAPI iapi  = new ESIndexAPI();
    private static final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();
	/**
	 * List of all sitesearch indicies
	 * @return
	 */
    @Override
	public  List<String> listIndices() {

		List<String> indices=new ArrayList<String>();
		for(String x:iapi.listIndices()){
			if(x.startsWith(ES_SITE_SEARCH_NAME)){
				indices.add(x);
			}
			
		}
		Collections.sort(indices);
		Collections.reverse(indices);
		return indices;
	}
	@Override
	public DotSearchResults search(String query, String sort, int start, int rows, String lang, String hostId) {
		
		return null;
	}
	
	
	@Override
    public void activateIndex(String indexName) throws DotDataException {
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

        if(indexName.startsWith(ES_SITE_SEARCH_NAME)) {
        	info.site_search=indexName;
        }

        APILocator.getIndiciesAPI().point(info);
    }
	
	@Override
    public void deactivateIndex(String indexName) throws DotDataException, IOException {
        IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();

        if(indexName.startsWith(ES_SITE_SEARCH_NAME)) {
        	info.site_search=null;
        }

        APILocator.getIndiciesAPI().point(info);
    }
	
	@Override
	public synchronized boolean createSiteSearchIndex(String indexName, int shards) throws ElasticSearchException, IOException {

		CreateIndexResponse cir = iapi.createIndex(indexName, null, shards);
		int i = 0;
		while(!cir.acknowledged()){
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if(i++ > 300){
				throw new ElasticSearchException("index timed out creating");
			}
		}
		
		
		ClassLoader classLoader = null;
		URL url = null;
		classLoader = Thread.currentThread().getContextClassLoader();
		url = classLoader.getResource("es-sitesearch-mapping.json");
        // create actual index
		String mapping = new String(com.liferay.util.FileUtil.getBytes(new File(url.getPath())));
			
		mappingAPI.putMapping(indexName, "content", mapping);
			
		
		
		return true;
	}
	
	
	
	
	
}
