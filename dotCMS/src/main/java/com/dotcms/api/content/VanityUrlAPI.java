package com.dotcms.api.content;

import java.util.List;

import com.dotcms.content.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

/**
 * This API provides access to the information related to Vanity URLs 
 * in dotCMS. Vanity URLs are alternate reference paths to
 * internal or external URL's. Vanity URLs are most commonly used to give
 * visitors to the website a more user-friendly or memorable way of reaching an
 * HTML page or File, that might actually live “buried” in a much deeper path.
 * 
 * @author oswaldogallango
 */
public interface VanityUrlAPI {
	/**
	 * This method load in cache all the active Vanity URLs 
	 * created in dotCMS
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void initializeActiveVanityURLsCache(User user) throws DotDataException, DotSecurityException;

	/**
	 * Get a list of all the existing Vanity URLs contents
	 * @param user The current user 
	 * @return a List of all the Vanity URLs contentlets
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<VanityUrl> getAllVanityUrls(User user) throws DotDataException, DotSecurityException;

	/**
	 * Get a list of all the Vanity URLs contents live
	 * @param user The current user
	 * @return a List of all Vanity URLs contentlets live
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<VanityUrl> getActiveVanityUrls(User user) throws DotDataException, DotSecurityException;

	/**
	 * Return the vanity URL contentlet with the specified URI
	 * @param uri The URI of the vanity URL
	 * @param host The curent host
	 * @param languageId The current language Id
	 * @param user The current user 
	 * @param live boolean indicating if the search should get only the live version of the vanity URLs
	 * @return the vanity URL contentlet
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	VanityUrl getVanityUrlByURI(String uri, Host host, long languageId, User user, boolean live) throws DotDataException, DotSecurityException;

	/**
	 * Convert the contentlet into a Vanity URL object
	 * @param con the contentlet
	 * @return Vanity URL
	 */
	VanityUrl fromContentlet(Contentlet con);
	
	/**
	 * Publishes (makes it live - available to view in the front-end)
	 * @param vanityUrlThe Vanity URL object
	 * @param user The current user
	 * @param respectFrontendRoles boolean to validate front end role permissions
	 * @throws DotContentletStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void publish(VanityUrl vanityUrl, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException;
	
	/**
	 * Un-publishes (makes it not available to view in the front-end)
	 * @param vanityUrlThe Vanity URL object
	 * @param user The current user
	 * @param respectFrontendRoles boolean to validate front end role permissions
	 * @throws DotContentletStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void unpublish(VanityUrl vanityUrl, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException;
	
	/**
	 * Marks the given Vanity URL as archived
	 * @param vanityUrlThe Vanity URL object
	 * @param user The current user
	 * @param respectFrontendRoles boolean to validate front end role permissions
	 * @throws DotContentletStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void archive(VanityUrl vanityUrl, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException;
	
	/**
	 * Un-marks the given Vanity URL as archived
	 * @param vanityUrlThe Vanity URL object
	 * @param user The current user
	 * @param respectFrontendRoles boolean to validate front end role permissions
	 * @throws DotContentletStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void unarchive(VanityUrl vanityUrl, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException;
	
	
	/**
	 * Removes the given Vanity URL
	 * @param vanityUrlThe Vanity URL object
	 * @param user The current user
	 * @param respectFrontendRoles boolean to validate front end role permissions
	 * @throws DotContentletStateException
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	void delete(VanityUrl vanityUrl, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException;
	
	/**
	 * Saves the Vanity URL into the system
	 */
	VanityUrl save(VanityUrl vanityUrl, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

}
