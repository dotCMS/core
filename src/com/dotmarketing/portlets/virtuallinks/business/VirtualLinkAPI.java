package com.dotmarketing.portlets.virtuallinks.business;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.liferay.portal.model.User;

public interface VirtualLinkAPI {
	public enum OrderBy {
		TITLE,
		DATE_ADDED,
		URL
	}

	/**
	 * Get all the virtual links filtered by the title and/or url.
	 *
	 * @param title
	 * @param url
	 * @param orderby
	 * @return List<VirtualLink>
	 */
	public List<VirtualLink> getVirtualLinks(String title, String url, OrderBy orderby);

	/**
	 * Copies a virtual link and moves it to the passed host
	 * @param sourceVirtualLink
	 * @return
	 * @throws DotHibernateException
	 */
	public VirtualLink copyVirtualLink(VirtualLink sourceVirtualLink, Host destinationHost) throws DotHibernateException;

	/**
	 * Retrieves all virtual links associated to a host
	 * @param host
	 * @return
	 */
	public List<VirtualLink> getHostVirtualLinks(Host host);


	/**
	 * Get all the virtual links filtered by the title and/or the list of hosts.
	 *
	 * @param title
	 * @param hosts
	 * @param orderby
	 * @return
	 */
	public List<VirtualLink> getVirtualLinks(String title, List<Host> hosts, OrderBy orderby);

	/**
	 *
	 * @param list
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<VirtualLink> checkListForCreateVirtualLinkspermission(java.util.List<VirtualLink> list,User user) throws DotDataException, DotSecurityException;

	/**
	 *
	 * @param link
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public VirtualLink checkVirtualLinkForEditPermissions(VirtualLink link,User user) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves all virtual links with the specified URI
	 * @param host
	 * @return
	 */
	public List<VirtualLink> getVirtualLinksByURI(String uri);
}