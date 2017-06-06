package com.dotcms.publisher.endpoint.business;

import java.util.List;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotmarketing.exception.DotDataException;

/**
 * This class provides methods to interact with publishing end-points in the
 * system. Publishing environments are composed of one or more publishing
 * end-points which represent the servers that dotCMS can send bundles to (HTML
 * pages, contents, files, folders, and so on).
 *
 * @author Graziano Aliberti
 * @since Oct 26, 2012
 * 
 */
public abstract class PublishingEndPointFactory {

	// this query is for show the end point list on UI
	protected static String GET_END_POINTS 						= 	"SELECT id, group_id, server_name, address, port, protocol, enabled, auth_key, sending " +
			"FROM publishing_end_point order by id, group_id, server_name";

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

	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public abstract List<PublishingEndPoint> getEndPoints() throws DotDataException;

	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public abstract List<PublishingEndPoint> getReceivingEndPoints() throws DotDataException;

	/**
	 * Returns the publishing end-point associated to the specified ID.
	 * 
	 * @param id
	 *            - The end-point's ID.
	 * @return The associated {@link PublishingEndPoint}
	 * @throws DotDataException
	 *             An error occurred when accessing the data source.
	 */
	public abstract PublishingEndPoint getEndPointById(String id) throws DotDataException;

	/**
	 * 
	 * @param name
	 * @return
	 * @throws DotDataException
	 */
	public abstract PublishingEndPoint getEndPointByName(String name) throws DotDataException;

	/**
	 * 
	 * @param address
	 * @return
	 * @throws DotDataException
	 */
	public abstract PublishingEndPoint getEnabledSendingEndPointByAddress(String address) throws DotDataException;

	/**
	 * 
	 * @param environmentId
	 * @return
	 * @throws DotDataException
	 */
	public abstract List<PublishingEndPoint> getSendingEndPointsByEnvironment(String environmentId) throws DotDataException;

	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public abstract List<PublishingEndPoint> getEnabledReceivingEndPoints() throws DotDataException;

	/**
	 * 
	 * @param anEndPoint
	 * @throws DotDataException
	 */
	public abstract void store(PublishingEndPoint anEndPoint) throws DotDataException;

	/**
	 * 
	 * @param anEndPoint
	 * @throws DotDataException
	 */
	public abstract void update(PublishingEndPoint anEndPoint) throws DotDataException;

	/**
	 * 
	 * @param id
	 * @throws DotDataException
	 */
	public abstract void deleteEndPointById(String id) throws DotDataException;

	/**
	 * 
	 * @return
	 * @throws DotDataException
	 */
	public abstract List<String> findSendGroups() throws DotDataException;

}
