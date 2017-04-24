package com.dotmarketing.portlets.virtuallinks.factories;

import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkAPI;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkFactoryImpl;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;

/**
 * A static Virtual Link Factory wrapper used for specific purposes only. <b>NOT
 * FOR COMMON USE. IT WILL BE REMOVED/REFACTORED IN THE FUTURE</b>.
 * 
 * @author will
 * @deprecated This class is a static wrapper of the
 *             {@link com.dotmarketing.portlets.virtuallinks.business.VirtualLinkFactory}
 *             class needed by the {@link VirtualLinksCache} during the startup
 *             process in order to avoid errors. This class <b>MUST NOT</b> be
 *             used as a Virtual Link Factory neither for dotCMS code nor for
 *             custom code. Please refer to the {@link VirtualLinkAPI} if you
 *             want to interact with Vanity URLs.
 */
public class VirtualLinkFactory {

	public static java.util.List<VirtualLink> getIncomingVirtualLinks(String uri) {
    	return new VirtualLinkFactoryImpl().getIncomingVirtualLinks(uri);
    }

    public static VirtualLink getVirtualLinkByURL(String url) throws DotHibernateException {
    	return new VirtualLinkFactoryImpl().getVirtualLinkByURL(url);
    }

	public static java.util.List<VirtualLink> getVirtualLinks() {
    	return new VirtualLinkFactoryImpl().getActiveVirtualLinks();
    }

    public static VirtualLink newInstance() {
        VirtualLink vl = new VirtualLink();
        vl.setActive(true);
        return vl;
    }

    public static VirtualLink getVirtualLink(String inode) {
    	return new VirtualLinkFactoryImpl().getVirtualLink(inode);
    }

	public static java.util.List<VirtualLink> getVirtualLinks(String condition,String orderby) {
		return new VirtualLinkFactoryImpl().getVirtualLinks(condition, orderby);
	}

}
