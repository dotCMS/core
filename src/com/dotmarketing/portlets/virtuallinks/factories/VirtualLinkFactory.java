package com.dotmarketing.portlets.virtuallinks.factories;

import static com.dotmarketing.business.PermissionAPI.PERMISSION_CREATE_VIRTUAL_LINKS;

import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/**
 * 
 * @author will
 */
public class VirtualLinkFactory {
    
    @SuppressWarnings("unchecked")
	public static java.util.List<VirtualLink> getIncomingVirtualLinks(String uri) {
    	java.util.List<VirtualLink> result = null;
        HibernateUtil dh = new HibernateUtil(VirtualLink.class);
        try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link' and uri = ? and active = "
			        + com.dotmarketing.db.DbConnectionFactory.getDBTrue());
			dh.setParam(uri);
			result = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactory.class, "getIncomingVirtualLinks failed:" + e, e);
		}
        return result;
    }

    public static VirtualLink getVirtualLinkByURL(String url) throws DotHibernateException {
        HibernateUtil dh = new HibernateUtil(VirtualLink.class);
        dh.setQuery("from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where url = ?");
        dh.setParam(url);
        return (VirtualLink) dh.load();
    }

    @SuppressWarnings("unchecked")
	public static java.util.List<VirtualLink> getVirtualLinks() {
        HibernateUtil dh = new HibernateUtil(VirtualLink.class);
        List<VirtualLink> list  =null; 
        try {
			dh.setQuery("from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link' and active = "
			        + com.dotmarketing.db.DbConnectionFactory.getDBTrue());
			list = dh.list();
		} catch (DotHibernateException e) {
		 Logger.error(VirtualLinkFactory.class, "getVirtualLinks failed:" + e, e);
		}
        return list;
    }

    public static VirtualLink newInstance() {
        VirtualLink vl = new VirtualLink();
        vl.setActive(true);
        return vl;

    }

    public static VirtualLink getVirtualLink(String inode) {
        return (VirtualLink) InodeFactory.getInode(inode, VirtualLink.class);
    }

	public static java.util.List<VirtualLink> getVirtualLinks(String condition,String orderby) {
		HibernateUtil dh = new HibernateUtil(VirtualLink.class);
		List<VirtualLink> list=null;
		String query ="from inode in class com.dotmarketing.portlets.virtuallinks.model.VirtualLink where type='virtual_link' ";
			if(condition!=null)
				query += " and (url like '%"+condition.toLowerCase()+"%' "+ "or title like '%"+condition.toLowerCase()+"%')";
			query += " and active = "+com.dotmarketing.db.DbConnectionFactory.getDBTrue();
			if(orderby!=null)
				query += " order by "+orderby;
        try {
			dh.setQuery(query);
			list = dh.list();
		} catch (DotHibernateException e) {
			Logger.error(VirtualLinkFactory.class, "getVirtualLinks failed:" + e, e);
		}
        return list;
	}

}