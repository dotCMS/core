package com.dotcms.publisher.endpoint.business;

import java.util.List;
import java.util.Map;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;

/**
 * This class provides a caching mechanism for Push Publishing End-Points in
 * dotCMS. This allows the system to avoid accessing the database constantly in
 * order to retrieve information.
 * 
 * @author Brent Griffin
 * @since Jan 28, 2013
 *
 */
public interface PublishingEndPointCache {

	/**
	 * Verifies if the cache is loaded, i.e., if it contains data.
	 * 
	 * @return Returns {@code true} if there's data in the cache. Otherwise,
	 *         returns {@code false}.
	 */
	public boolean isLoaded();

	/**
	 * Updates the load status of the cache.
	 * 
	 * @param isLoaded
	 *            - Set to {@code true} if data is added to the cache.
	 *            Otherwise, set to {@code false}.
	 */
	public void setLoaded(boolean isLoaded);

	/**
	 * Returns the list of {@link PublishingEndPoint} objects in the cache.
	 * 
	 * @return The list of {@link PublishingEndPoint} objects.
	 */
	public List<PublishingEndPoint> getEndPoints();

	/**
	 * Adds a single {@link PublishingEndPoint} to the cache.
	 * 
	 * @param anEndPoint
	 *            - The end-point that will be added to the cache.
	 */
	public void add(PublishingEndPoint anEndPoint);

	/**
	 * Adds the specified map of {@link PublishingEndPoint} objects to the
	 * cache. The map key corresponds to the end-point ID.
	 * 
	 * @param endPoints
	 *            - The end-points that will be added to the cache.
	 */
	public void addAll(Map<String, PublishingEndPoint> endPoints);

	/**
	 * Removes a specific end-point ID from the cache.
	 * 
	 * @param id
	 *            - The ID of the end-point that will be removed.
	 */
	public void removeEndPointById(String id);

	/**
	 * Removes all the objects stored by this cache.
	 */
	public void clearCache();

}
