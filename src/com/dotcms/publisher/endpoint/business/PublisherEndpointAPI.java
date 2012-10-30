package com.dotcms.publisher.endpoint.business;

import java.util.List;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.exception.DotDataException;

/**
 * API for Endpoints management.
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Oct 26, 2012 - 9:54:57 AM
 */
public interface PublisherEndpointAPI {
	
	/**
	 * Returns all endpoints configured into the system.
	 * 
	 * Oct 26, 2012 - 9:59:11 AM
	 */
	List<PublishingEndPoint> getAllEndpoints() throws DotDataException;
	
	/**
	 * Returns the single endpoint by id. Null otherwise. 
	 * 
	 * Oct 29, 2012 - 10:07:36 AM
	 */
	PublishingEndPoint findEndpointById(String id) throws DotDataException;
	
	/**
	 * Returns the endpoint configured like sender. Null otherwise. 
	 * 
	 * Oct 29, 2012 - 10:07:36 AM
	 */
	PublishingEndPoint findSenderEndpointByAddress(String address) throws DotDataException;
	
	/**
	 * Returns all the receiver endpoints. 
	 * 
	 * Oct 29, 2012 - 10:07:36 AM
	 */
	List<PublishingEndPoint> findReceiverEndpoints() throws DotDataException;
	
	/**
	 * Save a new endpoint.
	 * 
	 * Oct 29, 2012 - 12:45:37 PM
	 */
	void saveEndpoint(PublishingEndPoint anEndpoint) throws DotDataException;
	
	/**
	 * Update endpoint.
	 * 
	 * Oct 29, 2012 - 12:45:37 PM
	 */
	void updateEndpoint(PublishingEndPoint anEndpoint) throws DotDataException;
	
	/**
	 * Delete endpoint by identifier.
	 * 
	 * Oct 29, 2012 - 12:45:37 PM
	 */
	void deleteEndpointById(String id) throws DotDataException;
	
	
	
		
}
