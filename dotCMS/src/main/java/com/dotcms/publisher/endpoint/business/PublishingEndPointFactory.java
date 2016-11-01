package com.dotcms.publisher.endpoint.business;

import java.util.List;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.exception.DotDataException;

/**
 * Factory for manage data into publishing_end_point
 *
 * @author Graziano Aliberti - Engineering Ingegneria Informatica S.p.a
 *
 * Oct 26, 2012 - 10:04:20 AM
 */
public abstract class PublishingEndPointFactory {

	// this query is for show the end point list on UI
	protected static String GET_END_POINTS 						= 	"SELECT id, group_id, server_name, address, port, protocol, enabled, auth_key, sending " +
			"FROM publishing_end_point order by group_id, server_name";

	// this query is for store a new end point
	protected static String SET_END_POINT						=	"INSERT INTO publishing_end_point VALUES (?,?,?,?,?,?,?,?,?)";

	// this query is for update an existing end point
	protected static String UPDATE_END_POINT						=	"UPDATE publishing_end_point " +
																	"SET group_id = ?, server_name = ?, address = ?, " +
																	"	 port = ?, protocol = ?, enabled = ?, " +
																	"	 auth_key = ?, sending = ? " +
																	"WHERE id = ?";

	// this query is for delete an end point by id
	protected static String DELETE_END_POINT_BY_ID				=	"DELETE FROM publishing_end_point " +
			"WHERE id = ?";

	protected static String SELECT_END_POINT_BY_NAME				=	"SELECT * FROM publishing_end_point " +
																	"WHERE server_name = ?";

	public abstract List<PublishingEndPoint> getEndPoints() throws DotDataException;

	public abstract List<PublishingEndPoint> getReceivingEndPoints() throws DotDataException;

	public abstract PublishingEndPoint getEndPointById(String id) throws DotDataException;

	public abstract PublishingEndPoint getEndPointByName(String name) throws DotDataException;

	public abstract PublishingEndPoint getEnabledSendingEndPointByAddress(String address) throws DotDataException;

	public abstract List<PublishingEndPoint> getSendingEndPointsByEnvironment(String environmentId) throws DotDataException;

	public abstract List<PublishingEndPoint> getEnabledReceivingEndPoints() throws DotDataException;

	public abstract void store(PublishingEndPoint anEndPoint) throws DotDataException;

	public abstract void update(PublishingEndPoint anEndPoint) throws DotDataException;

	public abstract void deleteEndPointById(String id) throws DotDataException;

	public abstract List<String> findSendGroups() throws DotDataException;
}
