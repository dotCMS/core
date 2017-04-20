package com.dotmarketing.portlets.virtuallinks.business;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/**
 * Implementation class for the {@link VirtualLinkFactory}.
 * 
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 *
 */
public class VirtualLinkFactoryImpl implements VirtualLinkFactory {

	/**
	 * Returns the appropriate column name associated to the specified order-by
	 * Enum.
	 * 
	 * @param orderby
	 *            - Enum representing the column to order by.
	 * @return The correct column name.
	 */
	private String getOrderByField(VirtualLinkAPI.OrderBy orderby) {
		switch (orderby) {
			case TITLE:
				return "title";
			case DATE_ADDED:
				return "iDate";
			case URL:
				return "url";
			default:
				return null;
		}
	}

	@Override
	public List<VirtualLink> getVirtualLinks(String title, String url, VirtualLinkAPI.OrderBy orderby) {
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		java.util.List<VirtualLink> result =null;
		String query = "from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link'";

		if (UtilMethods.isSet(title)){
			query += " and lower(title) like ?";
		}
		if (UtilMethods.isSet(url)){
			query += " and (url like ? or url like ?)";
		}

		query += " and active = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue();
		if (orderby != null) {
			query += " order by " + getOrderByField(orderby);
		}
        try {
			dh.setQuery(query);
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class, e.getMessage(),e);
		}

        if (UtilMethods.isSet(title)){
        	dh.setParam("%" + title.toLowerCase() + "%");
        }
        if (UtilMethods.isSet(url)){
        	dh.setParam("%" + url.toLowerCase()+ "%");
        	dh.setParam("%/%");
        }

        try {
			result = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class,
					String.format("Method getVirtualLinks with title=[%s], url=[%s], orderby=[%s] failed: %s", title,
							url, orderby.toString(), e.getMessage()),
					e);
		}

        return result;
	}

	@Override
	public List<VirtualLink> getHostVirtualLinks(Host site) {
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		java.util.List<VirtualLink> result = null;
		String query = "from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link' and url like ?";
		try {
			dh.setQuery(query);
			dh.setParam(site.getHostname().toLowerCase() + ":%");
			result = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class, String.format(
					"Method getHostVirtualLinks with site=[%s] failed: %s", site.getHostname(), e.getMessage()), e);
		}
        return result;
    }

	@Override
	public List<VirtualLink> getVirtualLinksByURI(String uri) {
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		java.util.List<VirtualLink> result = null;
		if(!UtilMethods.isSet(uri)) return null;

		String query = "from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link' and lower(uri) = ?";
		try {
			dh.setQuery(query);
			dh.setParam(uri.toLowerCase());
			result = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class,
					String.format("Method getVirtualLinksByURI with uri=[%s] failed: %s", uri, e.getMessage()), e);
		}
        return result;
    }

	@Override
	public List<VirtualLink> getVirtualLinks(String title, List<Host> sites, VirtualLinkAPI.OrderBy orderby) {
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		java.util.List<VirtualLink> result=null;
		String query = "from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link'";
		if (title != null) {
			query += " and lower(title) like ?";
		}
		if (sites != null) {
			StringBuilder filteredSites = new StringBuilder(128);
			filteredSites.ensureCapacity(32);
			for (Host site : sites) {
				if (filteredSites.length() == 0) {
					filteredSites.append("url like ?");
				} else {
					filteredSites.append(" or url like ?");
				}
			}
			if (0 < sites.size()) {
				query += " and (" + filteredSites.toString() + ")";
			}
		}
		query += " and active = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue();
		if (orderby != null) {
			query += " order by " + getOrderByField(orderby);
		}
        try {
			dh.setQuery(query);
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class, e.getMessage(),e);
		}
        if (title != null) {
        	dh.setParam("%" + title.toLowerCase() + "%");
        }
        if (sites != null) {
        	for (Host site : sites) {
        		if (site.isSystemHost()) {
        			dh.setParam("/%");
        		} else {
        			dh.setParam("%" + site.getHostname().toLowerCase() + ":/%");
        		}
        	}
        }
        try {
			result = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class,
					String.format("Method getVirtualLinks with title=[%s], orderby=[%s] failed: %s", title, orderby,
							e.getMessage()),
					e);
		}
		return result;
	}

	@Override
	public java.util.List<VirtualLink> getIncomingVirtualLinks(String uri) {
		java.util.List<VirtualLink> result = null;
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		try {
			dh.setQuery(
					"from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link' and uri = ? and active = "
							+ DbConnectionFactory.getDBTrue());
			dh.setParam(uri.toLowerCase());
			result = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactory.class,
					String.format("Method getIncomingVirtualLinks with uri=[%s] failed: %s", uri, e.getMessage()), e);
		}
		return result;
	}

	@Override
	public VirtualLink getVirtualLinkByURL(String url) throws DotHibernateException {
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		dh.setQuery("from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where url = ?");
		dh.setParam(url.toLowerCase());
		return (VirtualLink) dh.load();
	}

	@Override
	public List<VirtualLink> getActiveVirtualLinks() {
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		List<VirtualLink> list = null;
		try {
			dh.setQuery(
					"from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link' and active = "
							+ DbConnectionFactory.getDBTrue());
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactory.class, "Method getVirtualLinks failed: " + e, e);
		}
		return list;
	}

	@Override
	public VirtualLink getVirtualLink(String inode) {
		return (VirtualLink) InodeFactory.getInode(inode, VirtualLink.class);
	}

	@Override
	public List<VirtualLink> getVirtualLinks(String condition, String orderby) {
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		List<VirtualLink> list = null;
		String query = "from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link' ";
		if (condition != null) {
			query += " and (url like '%" + condition.toLowerCase() + "%' " + "or lower(title) like '%"
					+ condition.toLowerCase() + "%')";
		}
		query += " and active = " + DbConnectionFactory.getDBTrue();
		if (orderby != null) {
			query += " order by " + orderby;
		}
		try {
			dh.setQuery(query);
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactory.class,
					String.format("Method getVirtualLinks with condition=[%s], orderby=[%s] failed: %s", condition,
							orderby, e.getMessage()),
					e);
		}
		return list;
	}

	@Override
	public void save(VirtualLink vanityUrl) {
		final String completeUrl = vanityUrl.getUrl();
		final String url = completeUrl.split(VirtualLinkAPI.URL_SEPARATOR)[1];
		VirtualLinksCache.removePathFromCache(url);
		try {
			HibernateUtil.saveOrUpdate(vanityUrl);
		} catch (DotHibernateException e) {
			throw new DotRuntimeException(
					String.format("An error occurred when saving Vanirty URL with title=[%s], url=[%s]",
							vanityUrl.getTitle(), vanityUrl.getUrl()),
					e);
		}
		VirtualLinksCache.removePathFromCache(url);
		VirtualLinksCache.addPathToCache(vanityUrl);
	}

	@Override
	public void delete(VirtualLink vanityUrl) throws DotHibernateException {
		InodeFactory.deleteInode(vanityUrl);
		// Removes this URL from cache
		if (UtilMethods.isSet(vanityUrl.getUrl())) {
			VirtualLinksCache.removePathFromCache(vanityUrl.getUrl());
		}
	}
	
}
