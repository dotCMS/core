package com.dotmarketing.portlets.virtuallinks.business;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;

public interface VirtualLinkFactory {
	/**
	 * Get all the virtual links filtered by the title and/or url.
	 *
	 * @param title
	 * @param url
	 * @param orderby
	 * @return List<VirtualLink>
	 */
	public List<VirtualLink> getVirtualLinks(String title, String url, VirtualLinkAPI.OrderBy orderby);

	/**
	 * Retrieves all virtual links associated to a host
	 * @param host
	 * @return
	 */
	public List<VirtualLink> getHostVirtualLinks(Host host);

	/**
	 * Retrieves all virtual links associated to a host with the specified URI
	 * @param host
	 * @return
	 */
	public List<VirtualLink> getVirtualLinksByURI(String uri);


	/**
	 * Get all the virtual links filtered by the title and/or the list of hosts.
	 *
	 * @param title
	 * @param hosts
	 * @param orderby
	 * @return
	 */
	public List<VirtualLink> getVirtualLinks(String title, List<Host> hosts, VirtualLinkAPI.OrderBy orderby);
}