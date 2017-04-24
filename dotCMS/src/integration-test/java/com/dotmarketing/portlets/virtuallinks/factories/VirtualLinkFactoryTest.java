package com.dotmarketing.portlets.virtuallinks.factories;

import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.cache.VirtualLinksCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.virtuallinks.business.VirtualLinkFactory;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.liferay.portal.model.User;

/**
 * JUnits for VirtualLinkFactory
 * @author Oswaldo Gallango
 * @since 8/24/15
 *
 */
public class VirtualLinkFactoryTest extends IntegrationTestBase {
	
    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

	/**
	 * Testing {@link VirtualLinkFactory#getVirtualLinkByURL(String)}
	 *
	 * @see {@link VirtualLinkFactory}
	 */
	@Test
	public void getVirtualLinkByURL () {
		VirtualLinkFactory virtualLinkFactory = FactoryLocator.getVirtualLinkFactory();
		HostAPI hostAPI = APILocator.getHostAPI();
		try{
			//host name
			String hostName="demo.dotcms.com";
			//default virtual link url
			String url = "/Family-Weekend-Reg";

			User user = APILocator.getUserAPI().getSystemUser();
			Host host = hostAPI.findByName(hostName, user, false);
			String  completeUrl = host.getHostname() + ":" + url;
			
			VirtualLink vl =  new VirtualLink();
			vl.setTitle("Test-Vanity-URL-Not-case-sensitive");
			vl.setUrl(completeUrl.toLowerCase());;
			//get HTMLPage 
			String page = "http://demo.dotcms.com/products/index";
			vl.setUri(page);        

			HibernateUtil.saveOrUpdate(vl);

			//reset url to cache
			VirtualLinksCache.removePathFromCache(vl.getUrl());
			VirtualLinksCache.addPathToCache(vl);

			//test vanity url works with upper case url
			VirtualLink nvl = virtualLinkFactory.getVirtualLinkByURL(completeUrl);
			assertTrue(nvl.getUrl() != null);
			//test vanity url with lower case url
			nvl = virtualLinkFactory.getVirtualLinkByURL(completeUrl.toLowerCase());
			assertTrue(nvl.getUri() != null);
		}catch(Exception e){
			assertTrue(e.getMessage(),false);
		}
	}

}
