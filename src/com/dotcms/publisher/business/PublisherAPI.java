package com.dotcms.publisher.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * This class allow to add/update, delete and search error in the publishing_queue table
 * from the actionlets
 * @author Oswaldo
 *
 */
public abstract class PublisherAPI {

	/*Basic operations*/
	public static final long PROCESSED_ELEMENT=0;
	public static final long ADD_OR_UPDATE_ELEMENT=1;
	public static final long DELETE_ELEMENT=2;
	
	private static PublisherAPI solrAPI = null;
	public static PublisherAPI getInstance(){
		if(solrAPI == null){
			solrAPI = PublisherAPIImpl.getInstance();
		}
		return solrAPI;	
	}

	/**
	 * Include in the publishing_queue table the content to add or update in the PublishQueue Index
	 * @param con Contentlet
	 */
	public abstract void addContentToPublishQueue(Contentlet con) throws DotPublisherException;
	
	/**
	 * Include in the publishing_queue table the Lucene query used to get contents to publish
	 * @param con Contentlet
	 */
	public abstract void addContentsToPublishQueue(List<Contentlet> contents, String bundleId, boolean isLive) throws DotPublisherException;

	/**
	 * Include in the publishing_queue table the content to remove in the PublishQueue Index
	 * @param con Contentlet
	 */
	public abstract void removeContentFromPublishQueue(Contentlet con) throws DotPublisherException;
	
	/**
	 * Include in the publishing_queue table the content to remove in the PublishQueue Index
	 * @param identifier Contentlet identifier
	 * @param languageId contentlet languageId
	 */
	public abstract void removeContentFromPublishQueue(String identifier, long languageId) throws DotPublisherException;

	/**
	 * Get a list of all the elements in the publishing_queue table that could be processes because some error
	 * @return List<Map<String,Object>>
	 */
	public abstract List<Map<String,Object>> getQueueErrors() throws DotPublisherException;

	/**
	 * Get the total of all the elements in the publishing_queue table that could be processes because some error
	 * @param condition WHERE condition
	 * @param orderBy ORDER BY condition
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String,Object>> getQueueErrorsCounter(String condition, String orderBy) throws DotPublisherException;
	
	/**
	 * Get a list of all the elements in the publishing_queue table that could be processes because some error
	 * @param condition WHERE condition
	 * @param orderBy ORDER BY condition
	 * @param offset first row to return
	 * @param limit max number of rows to return
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String,Object>> getQueueErrorsPaginated(String condition, String orderBy, String offset, String limit) throws DotPublisherException;
	
	/**
	 * Get the total of All the Assets in the publishing_queue table paginated
	 * @param condition WHERE condition
	 * @param orderBy ORDER BY condition
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String,Object>> getPublishQueueQueueContentletsCounter(String condition, String orderBy) throws DotPublisherException;
	
	/**
	 * Get All the Assets in the publishing_queue table paginated
	 * @param condition WHERE condition
	 * @param orderBy ORDER BY condition
	 * @param offset first row to return
	 * @param limit max number of rows to return
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String,Object>> getPublishQueueQueueContentletsPaginated(String condition, String orderBy, String offset, String limit) throws DotPublisherException;
	
	/**
	 * Get the total of Assets not processed yet to update the PublishQueue index paginated
	 * @param condition WHERE condition
	 * @param orderBy ORDER BY condition
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String,Object>> getPublishQueueQueueContentletToProcessCounter(String condition, String orderBy) throws DotPublisherException;
	
	/**
	 * Get the Assets not processed yet to update the PublishQueue index paginated
	 * @param condition WHERE condition
	 * @param orderBy ORDER BY condition
	 * @param offset first row to return
	 * @param limit max number of rows to return
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String,Object>> getPublishQueueQueueContentletToProcessPaginated(String condition, String orderBy, String offset, String limit) throws DotPublisherException;
	
	/**
	 * Get the Assets not processed yet to update the PublishQueue index
	 * @return List<Map<String,Object>>
	 */
	public abstract List<Map<String,Object>> getPublishQueueQueueContentletToProcess() throws DotPublisherException;
	
	/**
	 * update element from publishing_queue table by id
	 * @param id ID of the element in the publishing_queue
	 * @param next_try date of the next intent to execute the query
	 * @param in_error bolean indication if there was an error
	 * @param last_results error message
	 * @return boolean
	 */
	public abstract void updateElementStatusFromPublishQueueQueueTable(long id, Date last_try,int num_of_tries, boolean in_error,String last_results ) throws DotPublisherException;
	
	/**
	 * Delete element from publishing_queue table by id
	 * @param id ID of the element in the table
	 * @return boolean
	 */
	public abstract void deleteElementFromPublishQueueQueueTable(long id) throws DotPublisherException;
	
	/**
	 * Delete all elements from publishing_queue table
	 * @return boolean
	 */
	public abstract void deleteAllElementsFromPublishQueueQueueTable() throws DotPublisherException;
}
