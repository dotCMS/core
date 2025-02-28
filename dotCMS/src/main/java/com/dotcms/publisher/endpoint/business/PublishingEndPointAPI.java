package com.dotcms.publisher.endpoint.business;

import java.util.List;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.exception.DotDataException;

/**
 * API for EndPoints management.
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Oct 26, 2012 - 9:54:57 AM
 */
public interface PublishingEndPointAPI {

	/**
	 * Returns all endPoints configured into the system.
	 *
	 * All endPoints are returned regardless of whether or not they are enabled.
	 */
	List<PublishingEndPoint> getAllEndPoints() throws DotDataException;

	/**
	 * Returns receiving endPoints configured into the system.
	 *
	 * All endPoints where (isSending() == false) are returned.
	 */
	List<PublishingEndPoint> getReceivingEndPoints() throws DotDataException;

	/**
	 * Returns the single endPoint by id. If an endPoint is not found matching id then null is returned.
	 *
	 */
	PublishingEndPoint findEndPointById(String id) throws DotDataException;
	/**
	 * Returns the single endPoint by serverName. If an endPoint is not found matching serverName then null is returned.
	 *
	 */
	PublishingEndPoint findEndPointByName(String name) throws DotDataException;

	/**
	 * Returns enabled, sending endPoint that matches the provided address.  If a match is not found, null is returned.
	 *
	 * To be returned, the endPoint must have the matching address, be enabled, and be configured as sending.
	 * If an endPoint does not match all three criteria, null is returned.
	 *
	 */
	PublishingEndPoint findEnabledSendingEndPointByAddress(String address) throws DotDataException;

	/**
	 * Returns a List with the sending endPoints related to the given environmentId.  If no match is found, an empty List is returned.
	 *
	 * @param environmentId
	 * @return
	 * @throws DotDataException
	 */

	public List<PublishingEndPoint> findSendingEndPointsByEnvironment(String environmentId) throws DotDataException;

	/**
	 * Returns all enabled receiving endPoints.
	 *
	 * All endPoints where (endPoint.isEnabled() && isSending() == false) are returned.
	 *
	 */
	List<PublishingEndPoint> getEnabledReceivingEndPoints() throws DotDataException;

	/**
	 * Save a new endPoint.
	 *
	 */
	void saveEndPoint(PublishingEndPoint anEndPoint) throws DotDataException;

	/**
	 * Update endPoint.
	 *
	 */
	void updateEndPoint(PublishingEndPoint anEndPoint) throws DotDataException;

	/**
	 * Delete endPoint by identifier.
	 *
	 */
	void deleteEndPointById(String id) throws DotDataException;
	/**
	 * Returns a distinct list of all send groups
	 * @return
	 * @throws DotDataException
	 */
	List<String> findSendGroups()throws DotDataException;

	/**
	 * Creates an endPoint of the specified protocol.
	 * @return PublishingEndPoint
	 */
	PublishingEndPoint createEndPoint(String protocol) ;
}
