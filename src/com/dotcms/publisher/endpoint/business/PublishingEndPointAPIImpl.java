package com.dotcms.publisher.endpoint.business;

import java.util.List;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.exception.DotDataException;

/**
 * Implementation of publishing_end_point API.
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Oct 26, 2012 - 9:59:53 AM
 */
public class PublishingEndPointAPIImpl implements PublishingEndPointAPI {

	@Override
	public List<String> findSendGroups() throws DotDataException {
		return publishingEndPointFactory.findSendGroups();
	}

	private PublishingEndPointFactory publishingEndPointFactory;

	public PublishingEndPointAPIImpl(PublishingEndPointFactory publishingEndPointFactory){
		this.publishingEndPointFactory = publishingEndPointFactory;
	}

	/**
	 * Returns the end points list.
	 */
	public List<PublishingEndPoint> getAllEndPoints() throws DotDataException{
		return publishingEndPointFactory.getEndPoints();
	}

	public List<PublishingEndPoint> getReceivingEndPoints() throws DotDataException{
		return publishingEndPointFactory.getReceivingEndPoints();
	}

	/**
	 * Returns a single end point based on id.
	 */
	public PublishingEndPoint findEndPointById(String id) throws DotDataException {
		return publishingEndPointFactory.getEndPointById(id);
	}

	/**
	 * Save a new end point
	 */
	public void saveEndPoint(PublishingEndPoint anEndPoint) throws DotDataException {
		publishingEndPointFactory.store(anEndPoint);
	}

	/**
	 * Update an end point
	 */
	public void updateEndPoint(PublishingEndPoint anEndPoint) throws DotDataException {
		publishingEndPointFactory.update(anEndPoint);
	}

	/**
	 * Delete an end point by id
	 */
	public void deleteEndPointById(String id) throws DotDataException {
		publishingEndPointFactory.deleteEndPointById(id);
	}

	/**
	 * Returns the single end point configured like sender. Null otherwise.
	 *
	 */
	public PublishingEndPoint findEnabledSendingEndPointByAddress(String address) throws DotDataException {
		return publishingEndPointFactory.getEnabledSendingEndPointByAddress(address);
	}

	/**
	 * Returns a single end point configured like sender. Null otherwise.
	 *
	 */
	public List<PublishingEndPoint> findSendingEndPointsByEnvironment(String environmentId) throws DotDataException {
		return publishingEndPointFactory.getSendingEndPointsByEnvironment(environmentId);

	}

	/**
	 * Returns all the receiver end points.
	 */
	public List<PublishingEndPoint> getEnabledReceivingEndPoints() throws DotDataException {
		return publishingEndPointFactory.getEnabledReceivingEndPoints();
	}

	public PublishingEndPointFactory getPublishingEndPointFactory() {
		return publishingEndPointFactory;
	}

	public void setPublishingEndPointFactory(
			PublishingEndPointFactory publishingEndPointFactory) {
		this.publishingEndPointFactory = publishingEndPointFactory;
	}

	@Override
	public PublishingEndPoint findEndPointByName(String name) throws DotDataException {
		return publishingEndPointFactory.getEndPointByName(name);
	}
}
