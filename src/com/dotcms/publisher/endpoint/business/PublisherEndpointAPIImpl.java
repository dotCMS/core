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
	 * Return the endpoints list.
	 */
	public List<PublishingEndPoint> getAllEndpoints() throws DotDataException{		
		return publisherEndpointFactory.getEndpoints();
	}

	public PublisherEndpointFactory getPublisherEndpointFactory() {
		return publisherEndpointFactory;
	}

	public void setPublisherEndpointFactory(
			PublisherEndpointFactory publisherEndpointFactory) {
		this.publisherEndpointFactory = publisherEndpointFactory;
	}

}
