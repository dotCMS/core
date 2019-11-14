package com.dotcms.publisher.business;

import com.dotcms.publisher.business.PublishAuditStatus.Status;
import java.util.Date;
import java.util.List;


/**
 * This class implements CRUD for the publish audit table
 * from the actionlets
 * @author Oswaldo
 *
 */
public abstract class PublishAuditAPI {

	
	private static PublishAuditAPI pubAPI = null;
	public static PublishAuditAPI getInstance(){
		if(pubAPI == null){
			pubAPI = PublishAuditAPIImpl.getInstance();
		}
		return pubAPI;	
	}
	
	//Insert
	/**
	 * Insert a publish audit status 
	 * @param pa
	 * @throws DotPublisherException
	 */
	public abstract void insertPublishAuditStatus(PublishAuditStatus pa) throws DotPublisherException;
	
	/**
	 * Update publish audit status
	 * @param bundleId
	 * @param newStatus
	 * @param history
	 * @throws DotPublisherException
	 */
	//Update
	public abstract void updatePublishAuditStatus(String bundleId, Status newStatus, PublishAuditHistory history) throws DotPublisherException;

    /**
     * Update publish audit status
     *
     * @param bundleId
     * @param newStatus
     * @param history
     * @param updateDates True if want to override the create_date and status_updated with the current date
     * @throws DotPublisherException
     */
    //Update
	public abstract void updatePublishAuditStatus(String bundleId, Status newStatus, PublishAuditHistory history, Boolean updateDates ) throws DotPublisherException;
	
	/**
	 * Remove publish audit row from publish_audit table
	 * @param bundleId
	 * @throws DotPublisherException
	 */
	//Delete
	public abstract void deletePublishAuditStatus(String bundleId) throws DotPublisherException;

	/**
	 * Remove a list of publish bundles.
	 * @param bundleIds {@link List}
	 * @return List of bundle id deleted
	 * @throws DotPublisherException
	 */
	//Delete
	public abstract List<String>  deletePublishAuditStatus(List<String> bundleIds) throws DotPublisherException;
	
	/**
	 * Get a publish status given the bundle identifier
	 * @param bundleId
	 * @return
	 * @throws DotPublisherException
	 */
	//Select
	public abstract PublishAuditStatus getPublishAuditStatus(String bundleId) throws DotPublisherException;
	
	/**
	 * Get all publish status
	 * @param bundleId
	 * @return
	 * @throws DotPublisherException
	 */
	//Select
	public abstract List<PublishAuditStatus> getAllPublishAuditStatus() throws DotPublisherException;
	
	
	/**
	 * Get the date of the last bundle sent
	 * @param bundleId
	 * @return
	 * @throws DotPublisherException
	 */
	//Select
	public abstract Date getLastPublishAuditStatusDate() throws DotPublisherException;
	
	/**
	 * Get all publish status paginated
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract List<PublishAuditStatus> getAllPublishAuditStatus(Integer limit, Integer offset) throws DotPublisherException;
	
	/**
	 * count all publish status
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract Integer countAllPublishAuditStatus() throws DotPublisherException;

	/**
	 * Gets all audit status not yet ended
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract List<PublishAuditStatus> getPendingPublishAuditStatus() throws DotPublisherException;

    /**
     * Updates the audit table with the bundle info and status
     *
     * @param endpointId
     * @param groupId
     * @param bundleFolder
     * @return
     * @throws DotPublisherException
     */
    public abstract PublishAuditStatus updateAuditTable ( String endpointId, String groupId, String bundleFolder ) throws DotPublisherException;

    /**
     * Updates the audit table with the bundle info and status
     *
     * @param endpointId
     * @param groupId
     * @param bundleFolder
     * @param updateDates  True if want to override the create_date and status_updated with the current date
     * @return
     * @throws DotPublisherException
     */
    public abstract PublishAuditStatus updateAuditTable ( String endpointId, String groupId, String bundleFolder, Boolean updateDates ) throws DotPublisherException;

    /**
     * Find out if the publisher is retrying to process the bundle.
     *
     * @return true if the bundle has PublishAuditStatus with a number of tries > 0, false
     * otherwise.
     */
    public abstract boolean isPublishRetry(final String bundleId);

}