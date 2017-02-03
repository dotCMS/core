package com.dotmarketing.viewtools;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.beans.GoogleMiniSearch;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.GoogleMiniUtils;
import com.dotmarketing.util.UtilMethods;

public class GoogleMiniApi implements ViewTool{
	
	public void init(Object obj) {
	}
	
	/**
	 * Searches under google mini and return the results in a special object
	 * 
	 * @param client A valid setup client in google mini
	 * @param collection The collection to search on
	 * @param subcollection Subcollection to search on
	 * @param query Search query as entered by the user
	 * @param metaquery Meta tags query, filter to the search by meta tags names and values I.E. key:value.key:value.key:value|key:value. Use . as AND. User | as OR
	 * @param start Start index use -1 if want all results
	 * @param num Number of results to show use -1 to show all
	 * @param autoFilter Let google mini auto filter results, google mini automatic filtering does Duplicate Snippet Filter and Duplicate Directory Filter
	 * @return
	 * @throws Exception
	 */
	public GoogleMiniSearch searchGoogleMini(String client, String collection, String subcollection, String query, 
			String metaquery, int start, int num, boolean autoFilter) throws Exception {
		return GoogleMiniUtils.searchGoogleMini(client, collection, subcollection, query, 
				metaquery, start, num, autoFilter); 
	}
	
	/**
	 * Searches under google custom and return the results in a special object
	 * 
	 * @param client A valid setup client in google mini (Null can be used if you want to use the default value 'GOOGLE_CUSTOM_SEARCH_CSEID' configured in 'dotmarketing-config.properties')
	 * @param query Search query as entered by the user
	 * @param start Start index use -1 if want all results
	 * @param num Number of results to show use -1 to show all (Maximum value is 20. If you request more than 20 results, only 20 results will be returned)
	 * @param autoFilter Let google mini auto filter results, google mini automatic filtering does Duplicate Snippet Filter and Duplicate Directory Filter
	 * @return
	 * @throws Exception
	 */
	public GoogleMiniSearch searchGoogleCustom(String client, String query, int start, int num, boolean autoFilter) throws Exception {
		if (!UtilMethods.isSet(client))
			client = Config.getStringProperty("GOOGLE_CUSTOM_SEARCH_CLIENT_DEFAULT");
		return GoogleMiniUtils.searchGoogleCustom(client, query, start, num, autoFilter); 
	}
}