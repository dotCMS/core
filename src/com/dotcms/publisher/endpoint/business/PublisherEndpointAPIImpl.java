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
public class PublisherEndpointAPIImpl implements PublisherEndpointAPI {
	
	private PublisherEndpointFactory publisherEndpointFactory;
	
	public PublisherEndpointAPIImpl(PublisherEndpointFactory publisherEndpointFactory){
		this.publisherEndpointFactory = publisherEndpointFactory;
	}
	
	/**
	 * Returns the endpoints list.
	 */
	public List<PublishingEndPoint> getAllEndpoints() throws DotDataException{		
		return publisherEndpointFactory.getEndpoints();
	}
	
	/**
	 * Returns a single endpoint based on id.
	 */
	public PublishingEndPoint findEndpointById(String id) throws DotDataException {		
		return publisherEndpointFactory.getEndpointById(id);
	}

	/**
	 * Save a new endpoint
	 */
	public void saveEndpoint(PublishingEndPoint anEndpoint) throws DotDataException {
		publisherEndpointFactory.store(anEndpoint);		
	}
	
	/**
	 * Update an endpoint
	 */
	public void updateEndpoint(PublishingEndPoint anEndpoint) throws DotDataException {
		publisherEndpointFactory.update(anEndpoint);		
	}

	/**
	 * Delete an endpoint by id
	 */
	public void deleteEndpointById(String id) throws DotDataException {		
		publisherEndpointFactory.deleteEndpointById(id);
	}
	
	/**
	 * Returns the single endpoint configured like sender. Null otherwise. 
	 * 
	 */
	public PublishingEndPoint findSenderEndpointByAddress(String address) throws DotDataException {
		return publisherEndpointFactory.getSenderEndpointByAddress(address);
	}

	/**
	 * Returns all the receiver endpoints. 
	 */
	public List<PublishingEndPoint> findReceiverEndpoints() throws DotDataException {
		return publisherEndpointFactory.getReceiverEndpoints();
	}

	public PublisherEndpointFactory getPublisherEndpointFactory() {
		return publisherEndpointFactory;
	}

	public void setPublisherEndpointFactory(
			PublisherEndpointFactory publisherEndpointFactory) {
		this.publisherEndpointFactory = publisherEndpointFactory;
	}
}
