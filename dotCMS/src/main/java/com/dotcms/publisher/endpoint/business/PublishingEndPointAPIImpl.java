package com.dotcms.publisher.endpoint.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.integritycheckers.IntegrityUtil;
import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.exception.DotDataException;

import java.util.List;

/**
 * Implementation of publishing_end_point API.
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Oct 26, 2012 - 9:59:53 AM
 */
public class PublishingEndPointAPIImpl implements PublishingEndPointAPI {

	final com.dotcms.publisher.endpoint.bean.factory.PublishingEndPointFactory factory = new com.dotcms.publisher.endpoint.bean.factory.PublishingEndPointFactory();
	private PublishingEndPointFactory publishingEndPointFactory;

	public PublishingEndPointAPIImpl(PublishingEndPointFactory publishingEndPointFactory){
		this.publishingEndPointFactory = publishingEndPointFactory;
	}

	@CloseDBIfOpened
	@Override
	public List<String> findSendGroups() throws DotDataException {
		return publishingEndPointFactory.findSendGroups();
	}


	/**
	 * Returns the end points list.
	 */
	@CloseDBIfOpened
	@Override
	public List<PublishingEndPoint> getAllEndPoints() throws DotDataException{
		return publishingEndPointFactory.getEndPoints();
	}

	@CloseDBIfOpened
	@Override
	public List<PublishingEndPoint> getReceivingEndPoints() throws DotDataException{
		return publishingEndPointFactory.getReceivingEndPoints();
	}

	/**
	 * Returns a single end point based on id.
	 */
	@CloseDBIfOpened
	@Override
	public PublishingEndPoint findEndPointById(String id) throws DotDataException {
		return publishingEndPointFactory.getEndPointById(id);
	}

	/**
	 * Save a new end point
	 */
	@WrapInTransaction
	@Override
	public void saveEndPoint(PublishingEndPoint anEndPoint) throws DotDataException {
		publishingEndPointFactory.store(anEndPoint);
	}

	/**
	 * Update an end point
	 */
	@WrapInTransaction
	@Override
	public void updateEndPoint(PublishingEndPoint anEndPoint) throws DotDataException {
		publishingEndPointFactory.update(anEndPoint);
	}

	/**
	 * Delete an end point by id
	 */
	@WrapInTransaction
	@Override
	public void deleteEndPointById(String id) throws DotDataException {
	    //Delete all conflicts reported for this Endpoint
	    final IntegrityUtil integrityUtil = new IntegrityUtil();
	    integrityUtil.completeDiscardConflicts(id);
	    //Delete the Endpoint
		publishingEndPointFactory.deleteEndPointById(id);
	}

	/**
	 * Returns the single end point configured like sender. Null otherwise.
	 *
	 */
	@CloseDBIfOpened
	@Override
	public PublishingEndPoint findEnabledSendingEndPointByAddress(String address) throws DotDataException {
		return publishingEndPointFactory.getEnabledSendingEndPointByAddress(address);
	}

	/**
	 * Returns a single end point configured like sender. Null otherwise.
	 *
	 */
	@CloseDBIfOpened
	@Override
	public List<PublishingEndPoint> findSendingEndPointsByEnvironment(String environmentId) throws DotDataException {
		return publishingEndPointFactory.getSendingEndPointsByEnvironment(environmentId);

	}

	/**
	 * Returns all the receiver end points.
	 */
	@CloseDBIfOpened
	@Override
	public List<PublishingEndPoint> getEnabledReceivingEndPoints() throws DotDataException {
		return publishingEndPointFactory.getEnabledReceivingEndPoints();
	}

	@CloseDBIfOpened
	@Override
	public PublishingEndPoint findEndPointByName(String name) throws DotDataException {
		return publishingEndPointFactory.getEndPointByName(name);
	}

	public PublishingEndPointFactory getPublishingEndPointFactory() {
		return publishingEndPointFactory;
	}

	public void setPublishingEndPointFactory(
			PublishingEndPointFactory publishingEndPointFactory) {
		this.publishingEndPointFactory = publishingEndPointFactory;
	}

	@Override
	public PublishingEndPoint createEndPoint(final String protocol) {
		return this.factory.getPublishingEndPoint(protocol);
	}
}
