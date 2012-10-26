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
	 * Return all endpoints configured into the system.
	 * 
	 * Oct 26, 2012 - 9:59:11 AM
	 */
	List<PublishingEndPoint> getAllEndpoints() throws DotDataException;
		
}
