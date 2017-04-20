package com.dotmarketing.portlets.virtuallinks.business;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CREATE_VIRTUAL_LINKS;

import java.util.ArrayList;
import java.util.List;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDuplicateDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * Implementation class for the {@link VirtualLinkAPI}.
 * 
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 *
 */
public class VirtualLinkAPIImpl implements VirtualLinkAPI {

	private VirtualLinkFactory virtualLinkFactory;
	private HostAPI siteAPI;

	/**
	 * Default class constructor.
	 */
	public VirtualLinkAPIImpl() {
		this(FactoryLocator.getVirtualLinkFactory(), APILocator.getHostAPI());
	}

	@VisibleForTesting
	public VirtualLinkAPIImpl(VirtualLinkFactory virtualLinkFactory, HostAPI siteAPI) {
		this.virtualLinkFactory = virtualLinkFactory;
		this.siteAPI = siteAPI;
	}

	@Override
	public List<VirtualLink> getVirtualLinks(String title, String url, OrderBy orderby) {
		return virtualLinkFactory.getVirtualLinks(title, url, orderby);
	}

	@Override
	public VirtualLink copyVirtualLink(VirtualLink sourceVirtualLink, Host destinationSite) throws DotHibernateException {
		VirtualLink newVirtualLink = new VirtualLink();
		newVirtualLink.setActive(sourceVirtualLink.isActive());
		newVirtualLink.setTitle(sourceVirtualLink.getTitle());
		String[] sourceURL = sourceVirtualLink.getUrl().split(URL_SEPARATOR);
		String sourceURI = sourceURL.length == 1?sourceURL[0]:sourceURL[1];
		newVirtualLink.setUrl(destinationSite.getHostname() + URL_SEPARATOR + sourceURI);
		newVirtualLink.setUri(sourceVirtualLink.getUri());
		HibernateUtil.saveOrUpdate(newVirtualLink);
		return newVirtualLink;
	}

	@Override
	public List<VirtualLink> getHostVirtualLinks(Host site) {
		return virtualLinkFactory.getHostVirtualLinks(site);
	}

	@Override
	public List<VirtualLink> getVirtualLinksByURI(String uri) {
		return virtualLinkFactory.getVirtualLinksByURI(uri);
	}

	@Override
	public List<VirtualLink> getVirtualLinks(String title, List<Host> sites, OrderBy orderby) {
		return virtualLinkFactory.getVirtualLinks(title, sites, orderby);
	}

	@Override
	public List<VirtualLink> checkListForCreateVirtualLinkspermission(List<VirtualLink> list,User user) throws DotDataException, DotSecurityException {
		HostAPI hostAPI=APILocator.getHostAPI();
		List <Host> sites = hostAPI.getHostsWithPermission(PERMISSION_CREATE_VIRTUAL_LINKS, false, user, false);
		List <VirtualLink> vlinks=new ArrayList<VirtualLink>();
		try {
			if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
				vlinks=list;
			} else {
				if (!sites.isEmpty()) {
					for(VirtualLink vlink : list) {
						for(Host site : sites){
							if(vlink.getUrl().startsWith(site.getHostname())){
								vlinks.add(vlink);
							}
						}
					}
				}
			}
		} catch (DotDataException e) {
			Logger.error(VirtualLinkAPIImpl.class,e.getMessage(),e);
		}
		return vlinks;
	}

	@Override
	public VirtualLink checkVirtualLinkForEditPermissions(VirtualLink link,User user) throws DotDataException, DotSecurityException {
		HostAPI hostAPI=APILocator.getHostAPI();
		List <Host> sites = hostAPI.getHostsWithPermission(PERMISSION_CREATE_VIRTUAL_LINKS, false, user, false);
		try {
			if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){
				return link;
			}
			for(Host site : sites){
				if(link.getUrl().startsWith(site.getHostname())){
					return link;
				}
			}
		} catch (DotDataException e) {
			Logger.error(VirtualLinkAPIImpl.class,e.getMessage(),e);
		}
		return null;
	}

	@Override
	public List<VirtualLink> getIncomingVirtualLinks(String uri) {
		return this.virtualLinkFactory.getIncomingVirtualLinks(uri);
	}

	@Override
	public VirtualLink getVirtualLinkByURL(String url) throws DotHibernateException {
		return this.virtualLinkFactory.getVirtualLinkByURL(url);
	}

	@Override
	public List<VirtualLink> getActiveVirtualLinks() {
		return this.virtualLinkFactory.getActiveVirtualLinks();
	}

	@Override
	public VirtualLink getVirtualLink(String inode) {
		return this.virtualLinkFactory.getVirtualLink(inode);
	}

	@Override
	public List<VirtualLink> getVirtualLinks(String condition, String orderby) {
		return this.virtualLinkFactory.getVirtualLinks(condition, orderby);
	}

	@Override
	public VirtualLink create(final String title, String url, final String uri, final boolean isActive, Host site,
			User user) {
		if (!url.startsWith("/")) {
			url = "/" + url;
		}
		if (url.trim().endsWith("/")) {
			url = url.trim().substring(0, url.trim().length() - 1);
		}
		final String completeUrl;
		if (InodeUtils.isSet(site.getIdentifier())) {
			try {
				final Host permissionedSite = this.siteAPI.find(site.getIdentifier(), user, Boolean.FALSE);
				completeUrl = permissionedSite.getHostname() + URL_SEPARATOR + url;
			} catch (DotDataException | DotSecurityException e) {
				throw new DotRuntimeException(String.format("User [%s] does not have permissions to read site [%s]",
						user.getUserId(), site.getHostname()), e);
			}
		} else {
			throw new DotRuntimeException("The site Identifier is empty.");
		}
		final VirtualLink virtualLink = new VirtualLink();
		virtualLink.setTitle(title);
		virtualLink.setUrl(completeUrl);
		virtualLink.setUri(uri);
		virtualLink.setActive(isActive);
		return virtualLink;
	}

	@Override
	public void save(VirtualLink vanityUrl, User user) throws DotDataException, DotSecurityException {
		final VirtualLink existingVanityUrl = getVirtualLinkByURL(vanityUrl.getUrl());
		if (null != existingVanityUrl && InodeUtils.isSet(existingVanityUrl.getInode())
				&& !vanityUrl.getInode().equalsIgnoreCase(existingVanityUrl.getInode())) {
			throw new DotDuplicateDataException(
					String.format("Vanity URL with URL=[%s] already exists.", vanityUrl.getUrl()));
		}
		vanityUrl = checkVirtualLinkForEditPermissions(vanityUrl, user);
		if (null == vanityUrl) {
			throw new DotSecurityException(
					String.format("User [%s] does not have permission to perform this action.", user.getUserId()));
		}
		this.virtualLinkFactory.save(vanityUrl);
	}

	@Override
	public void delete(VirtualLink vanityUrl, User user) throws DotDataException, DotSecurityException {
		vanityUrl = checkVirtualLinkForEditPermissions(vanityUrl, user);
		if (null == vanityUrl) {
			throw new DotSecurityException(
					String.format("User [%s] does not have permission to perform this action.", user.getUserId()));
		}
		this.virtualLinkFactory.delete(vanityUrl);
	}

}
