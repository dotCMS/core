package com.dotcms.publisher.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.liferay.portal.model.User;

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
	public static final long ADD_OR_UPDATE_AND_DELETE=3;

	public static final long TO_PUBLISH_FILTER=1;
	public static final long TO_UNPUBLISH_FILTER=2;
	public static final long ERRORS_FILTER=3;

	// category
	protected static final String CATEGORY="CAT";

	private static PublisherAPI pubAPI = null;
	public static PublisherAPI getInstance(){
		if(pubAPI == null){
			pubAPI = PublisherAPIImpl.getInstance();
		}
		return pubAPI;
	}

	/**
	 * Include in the publishing_queue table the identifier used to get contents to publish
	 */
	public abstract Map<String, Object> addContentsToPublish(List<String> identifiers, String bundleId, Date publishDate, User user) throws DotPublisherException;

	/**
	 * Include in the publishing_queue table the identifier used to get contents to UN-publish
	 */
	public abstract Map<String, Object> addContentsToUnpublish(List<String> identifiers, String bundleId, Date unpublishDate, User user) throws DotPublisherException;

	/**
	 * Persists the relationship between a List of assets and the bundle that they were added to
	 */
	public abstract Map<String, Object> saveBundleAssets(List<String> identifiers, String bundleId, User user) throws DotPublisherException;

	/**
	 * sets the publish date and the publish operation type to the elements of the publishing queue contained in the bundle with the given bundleId
	 */
	public abstract void publishBundleAssets(String bundleId, Date publishDate) throws DotPublisherException;

	/**
	 * sets the expire date and the unpublish operation type to the elements of the publishing queue contained in the bundle with the given bundleId
	 */
	public abstract void unpublishBundleAssets(String bundleId, Date expireDate) throws DotPublisherException;

	/**
	 * sets the publish and expire date and the publish and expire operations types to the elements of the publishing queue contained in the bundle with the given bundleId
	 */
	public abstract void publishAndExpireBundleAssets(String bundleId, Date publishDate, Date expireDate, User user) throws DotPublisherException;


	/**
	 * Get tree data of a content
	 * @param id
	 * @return
	 */
	public abstract List<Map<String,Object>> getContentTreeMatrix(String id) throws DotPublisherException;

	/**
	 * Get multi tree data of a content
	 * @param id
	 * @return
	 */
	public abstract List<Map<String,Object>> getContentMultiTreeMatrix(String id) throws DotPublisherException;

	/**
	 * Get multi tree data of a container
	 * @param id
	 * @return
	 */
	public abstract List<Map<String,Object>> getContainerMultiTreeMatrix(String id) throws DotPublisherException;

	/**
	 * Get all elements of the queue table
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract List<PublishQueueElement> getQueueElements() throws DotPublisherException;


	/**
	 * count queue elements
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public abstract Integer countQueueElements() throws DotPublisherException;


	/**
	 * Get queue elements group by bundle_id
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String,Object>> getQueueElementsGroupByBundleId() throws DotPublisherException;

	/**
	 * gets a count of the distinct bundles
	 * @return
	 * @throws DotPublisherException
	 */

	public abstract Integer countQueueBundleIds() throws DotPublisherException;


	/**
	 * get bundle_ids available
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String,Object>> getQueueBundleIds(int limit, int offest) throws DotPublisherException ;

	/**
	 * Get the queue bundles to process
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String, Object>> getQueueBundleIdsToProcess() throws DotPublisherException;

	/**
	 * get queue elements by bundle_id
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public abstract List<PublishQueueElement> getQueueElementsByBundleId(String bundleId) throws DotPublisherException;

	/**
	 * count queue elements group by bundle_id
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public abstract Integer countQueueElementsGroupByBundleId() throws DotPublisherException;

	/**
	 * Get queue element by asset
	 * @param asset
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract List<PublishQueueElement> getQueueElementsByAsset(String asset) throws DotPublisherException;

	/**
	 * Get queue elements group by bundle_id paginated
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String,Object>> getQueueElementsGroupByBundleId(String offset, String limit) throws DotPublisherException;

	/**
	 * Get queue elements with a given status
	 * @param status
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract List<Map<String,Object>> getQueueElementsByStatus(Status status) throws DotPublisherException;

	/**
	 * update element from publishing_queue table by id
	 * @param id ID of the element in the publishing_queue
	 * @param next_try date of the next intent to execute the query
	 * @param in_error bolean indication if there was an error
	 * @param last_results error message
	 * @return boolean
	 */
	public abstract void updateElementStatusFromPublishQueueTable(long id, Date last_try,int num_of_tries, boolean in_error,String last_results ) throws DotPublisherException;

	/**
	 * Delete element from publishing_queue table by identifier
	 * @param id ID of the element in the table
	 * @return boolean
	 */
	public abstract void deleteElementFromPublishQueueTable(String identifier) throws DotPublisherException;

	/**
	 * Delete element(s) from publishing_queue table by bundleid
	 * @param id ID of the element in the table
	 * @return boolean
	 */
	public abstract void deleteElementsFromPublishQueueTable(String bundleId) throws DotPublisherException;

	/**
	 * Delete all elements from publishing_queue table
	 * @return boolean
	 */
	public abstract void deleteAllElementsFromPublishQueueTable() throws DotPublisherException;
}