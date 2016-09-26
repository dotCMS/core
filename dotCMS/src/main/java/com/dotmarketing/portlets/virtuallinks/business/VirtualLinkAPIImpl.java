package com.dotmarketing.portlets.virtuallinks.business;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CREATE_VIRTUAL_LINKS;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.virtuallinks.factories.VirtualLinkFactory;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class VirtualLinkAPIImpl implements VirtualLinkAPI {

	private com.dotmarketing.portlets.virtuallinks.business.VirtualLinkFactory virtualLinkFactory;

	public VirtualLinkAPIImpl() {
		virtualLinkFactory = FactoryLocator.getVirtualLinkFactory();
	}

	public List<VirtualLink> getVirtualLinks(String title, String url, OrderBy orderby) {
		return virtualLinkFactory.getVirtualLinks(title, url, orderby);
	}

	public VirtualLink copyVirtualLink(VirtualLink sourceVirtualLink, Host destinationHost) throws DotHibernateException {
		VirtualLink newVirtualLink = new VirtualLink();
		newVirtualLink.setActive(sourceVirtualLink.isActive());
		newVirtualLink.setTitle(sourceVirtualLink.getTitle());
		String[] sourceURL = sourceVirtualLink.getUrl().split(":");
		String sourceURI = sourceURL.length == 1?sourceURL[0]:sourceURL[1];
		newVirtualLink.setUrl(destinationHost.getHostname() + ":" + sourceURI);
		newVirtualLink.setUri(sourceVirtualLink.getUri());
		HibernateUtil.saveOrUpdate(newVirtualLink);
		return newVirtualLink;
	}

	public List<VirtualLink> getHostVirtualLinks(Host host) {
		return virtualLinkFactory.getHostVirtualLinks(host);
	}

	public List<VirtualLink> getVirtualLinksByURI(String uri) {
		return virtualLinkFactory.getVirtualLinksByURI(uri);
	}

	public List<VirtualLink> getVirtualLinks(String title, List<Host> hosts, OrderBy orderby) {
		return virtualLinkFactory.getVirtualLinks(title, hosts, orderby);
	}

	public java.util.List<VirtualLink> checkListForCreateVirtualLinkspermission(java.util.List<VirtualLink> list,User user) throws DotDataException, DotSecurityException {
		HostAPI hostAPI=APILocator.getHostAPI();
		List <Host>  hosts = hostAPI.getHostsWithPermission(PERMISSION_CREATE_VIRTUAL_LINKS, false, user, false);
		List <VirtualLink> vlinks=new ArrayList<VirtualLink>();

		try {
			if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){

				vlinks=list;
			}
			else for(VirtualLink vlink : list)
			{
				for(Host host: hosts){

					if(vlink.getUrl().startsWith(host.getHostname())){
						vlinks.add(vlink);
					}

				}
			}
		} catch (DotDataException e) {
			Logger.error(VirtualLinkFactory.class,e.getMessage(),e);
		}

		return vlinks;
	}


	public VirtualLink checkVirtualLinkForEditPermissions(VirtualLink link,User user) throws DotDataException, DotSecurityException {
		HostAPI hostAPI=APILocator.getHostAPI();
		List <Host>  hosts = hostAPI.getHostsWithPermission(PERMISSION_CREATE_VIRTUAL_LINKS, false, user, false);

		try {
			if(APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole())){

				return link;
			}
				for(Host host: hosts){

					if(link.getUrl().startsWith(host.getHostname())){
						return link;
					}

				}

		} catch (DotDataException e) {
			Logger.error(VirtualLinkFactory.class,e.getMessage(),e);
		}

		return null;
	}

}