package com.dotcms.publisher.business;

import com.dotcms.business.WrapInTransaction;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publishing.PublisherConfig.DeliveryStrategy;
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

	/**
	 * Returns a single instance of this API.
	 * 
	 * @return A single PublisherAPI instance.
	 */
	public static PublisherAPI getInstance(){
		if(pubAPI == null){
			pubAPI = PublisherAPIImpl.getInstance();
		}
		return pubAPI;
	}

	/**
	 * Takes a list of Identifiers associated to any pusheable asset and
	 * registers each of them into a bundle <b>for push publishing</b>.
	 * 
	 * @param identifiers
	 *            - The list of pusheable assets that will be included in the
	 *            bundle.
	 * @param bundleId
	 *            - The ID of the bundle.
	 * @param publishDate
	 *            - The date when the bundle will be pushed (usually, the
	 *            current date and time).
	 * @param user
	 *            - The {@link User} performing this action.
	 * @return A summary {@link Map} containing the result of the operation,
	 *         such as, error messages, total assets to push, etc.
	 * @throws DotPublisherException
	 *             An error occurred when adding a pusheable asset to the queue.
	 */
	public abstract Map<String, Object> addContentsToPublish(List<String> identifiers, String bundleId, Date publishDate, User user) throws DotPublisherException;

	/**
	 * Takes a list of Identifiers associated to any pusheable asset and
	 * registers each of them into a bundle <b>for publishing</b>. When
	 * re-trying to send a bundle that failed to publish, this method is more
	 * efficient as it allows you to send it to either <b>ALL</b> or
	 * <b>FAILED</b> end-points only.
	 * 
	 * @param identifiers
	 *            - The list of pusheable assets that will be included in the
	 *            bundle.
	 * @param bundleId
	 *            - The ID of the bundle.
	 * @param publishDate
	 *            - The date when the bundle will be pushed (usually, the
	 *            current date and time).
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param deliveryStrategy
	 *            - The {@link DeliveryStrategy} for the contents, i.e., send
	 *            the bundle to all end-points, or failed end-points only.
	 * @return A summary {@link Map} containing the result of the operation,
	 *         such as, error messages, total assets to push, etc.
	 * @throws DotPublisherException
	 *             An error occurred when adding a pusheable asset to the queue.
	 */
	public abstract Map<String, Object> addContentsToPublish(List<String> identifiers, String bundleId, Date publishDate, User user, DeliveryStrategy deliveryStrategy) throws DotPublisherException;

	/**
	 * Takes a list of Identifiers associated to any pusheable asset and
	 * registers each of them into a bundle <b>for un-publishing</b>.
	 * 
	 * @param identifiers
	 *            - The list of pusheable assets that will be included in the
	 *            bundle for un-publishing.
	 * @param bundleId
	 *            - The ID of the bundle.
	 * @param publishDate
	 *            - The date when the bundle will be pushed (usually, the
	 *            current date and time).
	 * @param user
	 *            - The {@link User} performing this action.
	 * @return A summary {@link Map} containing the result of the operation,
	 *         such as, error messages, total assets to push, etc.
	 * @throws DotPublisherException
	 *             An error occurred when adding a pusheable asset to the queue.
	 */
	public abstract Map<String, Object> addContentsToUnpublish(List<String> identifiers, String bundleId, Date unpublishDate, User user) throws DotPublisherException;

	/**
	 * Takes a list of Identifiers associated to any pusheable asset and
	 * registers each of them into a bundle <b>for un-publishing</b>. When
	 * re-trying to send a bundle that failed to un-publish, this method is more
	 * efficient as it allows you to send it to either <b>ALL</b> or
	 * <b>FAILED</b> end-points only.
	 * 
	 * @param identifiers
	 *            - The list of pusheable assets that will be included in the
	 *            bundle.
	 * @param bundleId
	 *            - The ID of the bundle.
	 * @param publishDate
	 *            - The date when the bundle will be pushed (usually, the
	 *            current date and time).
	 * @param user
	 *            - The {@link User} performing this action.
	 * @param deliveryStrategy
	 *            - The {@link DeliveryStrategy} for the contents, i.e., send
	 *            the bundle to all end-points, or failed end-points only.
	 * @return A summary {@link Map} containing the result of the operation,
	 *         such as, error messages, total assets to push, etc.
	 * @throws DotPublisherException
	 *             An error occurred when adding a pusheable asset to the queue.
	 */
	public abstract Map<String, Object> addContentsToUnpublish(List<String> identifiers, String bundleId, Date unpublishDate, User user, DeliveryStrategy deliveryStrategy) throws DotPublisherException;

	/**
	 * Persists the relationship between a List of assets and the bundle that
	 * they were added to.
	 * 
	 * @param identifiers
	 * @param bundleId
	 * @param user
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract Map<String, Object> saveBundleAssets(List<String> identifiers, String bundleId, User user) throws DotPublisherException;

	/**
	 * Sets the publish date and the publish operation type to the elements of
	 * the publishing queue contained in the bundle with the given bundleId.
	 * 
	 * @param bundleId
	 * @param publishDate
	 * @throws DotPublisherException
	 */
	public abstract void publishBundleAssets(String bundleId, Date publishDate) throws DotPublisherException;

	/**
	 * Sets the expire date and the unpublish operation type to the elements of
	 * the publishing queue contained in the bundle with the given bundleId.
	 * 
	 * @param bundleId
	 * @param expireDate
	 * @throws DotPublisherException
	 */
	public abstract void unpublishBundleAssets(String bundleId, Date expireDate) throws DotPublisherException;

	/**
	 * Sets the publish and expire date and the publish and expire operations
	 * types to the elements of the publishing queue contained in the bundle
	 * with the given bundleId.
	 * 
	 * @param bundleId
	 * @param publishDate
	 * @param expireDate
	 * @param user
	 * @throws DotPublisherException
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
	 * Deletes a record from the {@code publishing_queue} table based on its
	 * Identifier and language ID.
	 * 
	 * @param identifier
	 *            - The Identifier of the record to delete.
	 * @param languageId
	 *            - The language ID of the record to delete.
	 * @throws DotPublisherException
	 *             An error occurred when deleting the entry.
	 */
	public abstract void deleteElementFromPublishQueueTable(String identifier, long languageId) throws DotPublisherException;

	@WrapInTransaction
	public abstract void deleteElementsFromPublishQueueTable(List<String> identifier,
			long languageId) throws DotPublisherException;

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

	/**
	 * This will immediately fire the publishing quartz job. Configuration or
	 * execution parameters for the publishing mechanism can be specified.
	 * 
	 * @param dataMap
	 *            - A {@link Map} containing different configuration or
	 *            execution parameters for the publisher process.
	 */
    public abstract void firePublisherQueueNow(Map<String, Object> dataMap);

}
