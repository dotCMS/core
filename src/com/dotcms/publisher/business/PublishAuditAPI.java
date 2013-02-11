package com.dotcms.publisher.business;

import java.util.Date;
import java.util.List;

import com.dotcms.publisher.business.PublishAuditStatus.Status;


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
	 * Remove publish audit row from publish_audit table
	 * @param bundleId
	 * @throws DotPublisherException
	 */
	//Delete
	public abstract void deletePublishAuditStatus(String bundleId) throws DotPublisherException;
	
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
	 * @param endpointId
	 * @param groupId
	 * @param bundleFolder
	 * @return
	 * @throws DotPublisherException
	 */
	public abstract PublishAuditStatus updateAuditTable(String endpointId, String groupId, String bundleFolder)
			throws DotPublisherException;
}
