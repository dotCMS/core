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
	
	// this query is for show the endpoint list on UI
	protected static String GET_ENDPOINTS 						= 	"SELECT id, group_id, server_name, address, port, protocol, enabled, auth_key, sending " +
																	"FROM publishing_end_point";
	
	// this query is for select a single endpoint for edit it
	protected static String GET_ENDPOINT_BY_ID					=	"SELECT id, group_id, server_name, address, port, protocol, enabled, auth_key, sending " +
																	"FROM publishing_end_point " +
																	"WHERE id = ?";
	
	// this query is for select the current sender, on the receiver side, filtered by address
	protected static String GET_SENDER_ENDPOINT_BY_ADDRESS		=	"SELECT id, group_id, server_name, address, port, protocol, enabled, auth_key, sending " +
																	"FROM publishing_end_point " +
																	"WHERE sending = ? " +
																	"AND address = ? " +
																	"AND enabled = ?";
	
	// this query is for select ALL the enabled receiver endpoint for sending them the bundle
	protected static String GET_RECEIVER_ENDPOINTS				=	"SELECT id, group_id, server_name, address, port, protocol, enabled, auth_key, sending " +
																	"FROM publishing_end_point " +
																	"WHERE sending = ? " +
																	"AND enabled = ?";

	// this query is for store a new endpoint
	protected static String SET_ENDPOINT						=	"INSERT INTO publishing_end_point VALUES (?,?,?,?,?,?,?,?,?)";
	
	// this query is for update an existing endpoint
	protected static String UPDATE_ENDPOINT						=	"UPDATE publishing_end_point " +
																	"SET group_id = ?, server_name = ?, address = ?, " +
																	"	 port = ?, protocol = ?, enabled = ?, " +
																	"	 auth_key = ?, sending = ? " +
																	"WHERE id = ?";
	
	// this query is for delete an endpoint by id
	protected static String DELETE_ENDPOINT_BY_ID				=	"DELETE FROM publishing_end_point " +
																	"WHERE id = ?";		
	
	public abstract List<PublishingEndPoint> getEndpoints() throws DotDataException;
	
	public abstract PublishingEndPoint getEndpointById(String id) throws DotDataException;
	
	public abstract PublishingEndPoint getSenderEndpointByAddress(String address) throws DotDataException;
	
	public abstract List<PublishingEndPoint> getReceiverEndpoints() throws DotDataException;	
	
	public abstract void store(PublishingEndPoint anEndpoint) throws DotDataException;
	
	public abstract void update(PublishingEndPoint anEndpoint) throws DotDataException;
	
	public abstract void deleteEndpointById(String id) throws DotDataException;
	
	
}
