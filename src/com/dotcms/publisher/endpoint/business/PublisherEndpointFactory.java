package com.dotcms.publisher.endpoint.business;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.exception.DotDataException;

import java.util.List;

/**
 * Factory for manage data into publishing_end_point
 * 
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Oct 26, 2012 - 10:04:20 AM
 */
public abstract class PublisherEndpointFactory {
	
	protected static String GET_ENDPOINTS 						= 	"SELECT id, group_id, server_name, address, port, protocol, enabled, auth_key, sending " +
																	"FROM publishing_end_point";
	
	public abstract List<PublishingEndPoint> getEndpoints() throws DotDataException;
}
