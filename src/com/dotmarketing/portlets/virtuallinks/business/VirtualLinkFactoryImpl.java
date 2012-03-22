package com.dotmarketing.portlets.virtuallinks.business;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class VirtualLinkFactoryImpl implements VirtualLinkFactory {
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
	
	@SuppressWarnings("unchecked")
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
		if (orderby != null)
			query += " order by " + getOrderByField(orderby);
		
        try {
			dh.setQuery(query);
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class, e.getMessage(),e);
		}
        
        if (UtilMethods.isSet(title)){
        	dh.setParam("%" + title.toLowerCase() + "%");
        }
        
        if (UtilMethods.isSet(url)){
        	dh.setParam("%" + url+ "%");
        	dh.setParam("%/%");
        }
        
        try {
			result = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class, "getVirtualLinks failed:" + e,e);
		}
        
        return result; 
	}

	@SuppressWarnings("unchecked")
	public List<VirtualLink> getHostVirtualLinks(Host host) {
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		java.util.List<VirtualLink> result = null;
		String query = "from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link'";
		
		query += " and url like ?";
		try {
			dh.setQuery(query);
			dh.setParam(host.getHostname() + ":%");
			result = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class, "getHostVirtualLinks failed:" + e,e);
		}        
        return result; 	
    }
	
	
	public List<VirtualLink> getVirtualLinks(String title, List<Host> hosts, VirtualLinkAPI.OrderBy orderby) {
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		java.util.List<VirtualLink> result=null;
		String query = "from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link'";
		
		if (title != null)
			query += " and title like ?";
		
		if (hosts != null) {
			StringBuilder filterHosts = new StringBuilder(128);
			filterHosts.ensureCapacity(32);
			for (Host host: hosts) {
				if (filterHosts.length() == 0)
					filterHosts.append("url like ?");
				else
					filterHosts.append(" or url like ?");
			}
			
			if (0 < hosts.size())
				query += " and (" + filterHosts.toString() + ")";
		}
		
		query += " and active = " + com.dotmarketing.db.DbConnectionFactory.getDBTrue();
		if (orderby != null)
			query += " order by " + getOrderByField(orderby);
		
        try {
			dh.setQuery(query);
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class, e.getMessage(),e);
		}
        
        if (title != null)
        	dh.setParam("%" + title.toLowerCase() + "%");
        
        if (hosts != null) {
        	for (Host host: hosts) {
        		if (host.isSystemHost())
        			dh.setParam("/%");
        		else
        			dh.setParam("%" + host.getHostname() + ":/%");
        	}
        }
        
        try {
			result = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactoryImpl.class, "getVirtualLinks failed:" + e,e);
		}
		return result;
	}
}