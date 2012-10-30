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
	
	protected static String GET_ENDPOINT_BY_ID					=	"SELECT id, group_id, server_name, address, port, protocol, enabled, auth_key, sending " +
																	"FROM publishing_end_point " +
																	"WHERE id = ?";
	
	protected static String GET_SENDER_ENDPOINT					=	"SELECT id, group_id, server_name, address, port, protocol, enabled, auth_key, sending " +
																	"FROM publishing_end_point " +
																	"WHERE sending = 1";

	protected static String GET_RECEIVER_ENDPOINTS				=	"SELECT id, group_id, server_name, address, port, protocol, enabled, auth_key, sending " +
																	"FROM publishing_end_point " +
																	"WHERE sending = 0";

	
	protected static String SET_ENDPOINT						=	"INSERT INTO publishing_end_point VALUES (?,?,?,?,?,?,?,?,?)";
	
	protected static String UPDATE_ENDPOINT						=	"UPDATE publishing_end_point " +
																	"SET group_id = ?, server_name = ?, address = ?, " +
																	"	 port = ?, protocol = ?, enabled = ?, " +
																	"	 auth_key = ?, sending = ? " +
																	"WHERE id = ?";
	
	protected static String DELETE_ENDPOINT_BY_ID				=	"DELETE FROM publishing_end_point " +
																	"WHERE id = ?";		
	
	public abstract List<PublishingEndPoint> getEndpoints() throws DotDataException;
	
	public abstract PublishingEndPoint getEndpointById(String id) throws DotDataException;
	
	public abstract PublishingEndPoint getSenderEndpoint() throws DotDataException;
	
	public abstract List<PublishingEndPoint> getReceiverEndpoints() throws DotDataException;
	
	public abstract void store(PublishingEndPoint anEndpoint) throws DotDataException;
	
	public abstract void update(PublishingEndPoint anEndpoint) throws DotDataException;
	
	public abstract void deleteEndpointById(String id) throws DotDataException;
	
	
}
