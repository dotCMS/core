package com.dotcms.api.content;

import com.dotcms.content.model.VanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import java.util.List;

/**
 * This API provides access to the information related to Vanity URLs 
 * in dotCMS. Vanity URLs are alternate reference paths to
 * internal or external URL's. Vanity URLs are most commonly used to give
 * visitors to the website a more user-friendly or memorable way of reaching an
 * HTML page or File, that might actually live “buried” in a much deeper path.
 * 
 * @author oswaldogallango
 * @version 4.2.0
 * @since June 12, 2017
 */
public interface VanityUrlAPI {

	public static final String CACHE_404_VANITY_URL = "CACHE_404_VANITY_URL";
	/**
	 * Get a list of all the existing Vanity URLs contents
	 * @param user The current user 
	 * @return a List of all the Vanity URLs contentlets
	 */
	List<VanityUrl> getAllVanityUrls(final User user);

	/**
	 * Get a list of all the Vanity URLs contents live
	 * @param user The current user
	 * @return a List of all Vanity URLs contentlets live
	 */
	List<VanityUrl> getActiveVanityUrls(final User user);

	/**
	 * Return the vanity URL working contentlet with the specified URI
	 * @param uri The URI of the vanity URL
	 * @param host The current host
	 * @param languageId The current language Id
	 * @param user The current user 
	 * @return the working version of the vanity URL contentlet
	 */
	VanityUrl getWorkingVanityUrl(final String uri, final Host host, final long languageId, final User user);

	/**
	 * Return the live version of the vanity URL contentlet with the specified URI
	 * @param uri The URI of the vanity URL
	 * @param host The current host
	 * @param languageId The current language Id
	 * @param user The current user
	 * @return the live version of the vanity URL contentlet
	 */
	VanityUrl getLiveVanityUrl(final String uri, final Host host, final long languageId, final User user);

	/**
	 * Convert the contentlet into a Vanity URL object
	 * @param con the contentlet
	 * @return Vanity URL
	 */
	VanityUrl getVanityUrlFromContentlet(final Contentlet con);
}
