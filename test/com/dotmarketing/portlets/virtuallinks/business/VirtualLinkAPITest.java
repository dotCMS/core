package com.dotmarketing.portlets.virtuallinks.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cactus.ServletTestCase;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.liferay.portal.model.User;

public class VirtualLinkAPITest extends ServletTestCase {
	private static UserAPI userAPI;
	private static VirtualLinkAPI virtualLinkAPI;
	
	private static VirtualLink testVirtualLink1;
	private static VirtualLink testVirtualLink2;
	
	private static boolean active = true;
	private static Date iDate = new Date();
	private static String owner;
	private static String title1 = "JUnit Virtual Link Test 1";
	private static String type = "virtual_link";
	private static String uri1 = "/junit_virtuallink_test1";
	private static String testHostname1 = "junit_host_test1";
	private static String url1 = testHostname1 + ":/test1";
	
	private static String title2 = "JUnit Virtual Link Test 2";
	private static String uri2 = "/junit_virtuallink_test2";
	private static String testHostname2 = "junit_host_test2";
	private static String url2 = testHostname2 + ":/test2";
	
	protected void setUp() throws Exception {
		userAPI = APILocator.getUserAPI();
		virtualLinkAPI = APILocator.getVirtualLinkAPI();
		
		createJUnitTestVirtualLink();
	}
	
	protected void tearDown() throws Exception {
		deleteJUnitTestVirtualLink();
	}
	
	private void createJUnitTestVirtualLink() throws Exception {
		testVirtualLink1 = (VirtualLink) InodeFactory.getInode(null, VirtualLink.class);
		
		testVirtualLink1.setActive(active);
		testVirtualLink1.setIDate(iDate);
		
		User user = userAPI.getSystemUser();
		owner = user.getUserId();
		testVirtualLink1.setOwner(owner);
		
		testVirtualLink1.setTitle(title1);
		testVirtualLink1.setType(type);
		testVirtualLink1.setUri(uri1);
		testVirtualLink1.setUrl(url1);
		
		HibernateUtil.saveOrUpdate(testVirtualLink1);
		
		testVirtualLink2 = (VirtualLink) InodeFactory.getInode(null, VirtualLink.class);
		
		testVirtualLink2.setActive(active);
		testVirtualLink2.setIDate(iDate);
		testVirtualLink2.setOwner(owner);
		testVirtualLink2.setTitle(title2);
		testVirtualLink2.setType(type);
		testVirtualLink2.setUri(uri2);
		testVirtualLink2.setUrl(url2);
		
		HibernateUtil.saveOrUpdate(testVirtualLink2);
	}
	
	private void deleteJUnitTestVirtualLink() throws Exception {
		InodeFactory.deleteInode(testVirtualLink1);
		InodeFactory.deleteInode(testVirtualLink2);
	}
	
	public void testGetVirtualLinks() throws Exception {
		Host host1 = new Host();
		host1.setArchived(false);
		host1.setHostname(testHostname1);
		
		Host host2 = new Host();
		host2.setArchived(false);
		host2.setHostname(testHostname2);
		
		List<Host> hosts = new ArrayList<Host>();
		hosts.add(host1);
		hosts.add(host2);
		
		VirtualLinkAPI.OrderBy orderby = VirtualLinkAPI.OrderBy.TITLE;
		
		List<VirtualLink> virtualLinks = virtualLinkAPI.getVirtualLinks(null, hosts, orderby);
		assertEquals("Invalid number of virtual links pulled.", 2, virtualLinks.size());
		assertEquals("Invalid order of the virtual links pulled.", title1, virtualLinks.get(0).getTitle());
		
		virtualLinks = virtualLinkAPI.getVirtualLinks(title2, hosts, orderby);
		assertEquals("Invalid number of virtual links pulled.", 1, virtualLinks.size());
		assertEquals("Invalid virtual link pulled.", title2, virtualLinks.get(0).getTitle());
		
		hosts.remove(1);
		virtualLinks = virtualLinkAPI.getVirtualLinks(null, hosts, orderby);
		assertEquals("Invalid number of virtual links pulled.", 1, virtualLinks.size());
		assertEquals("Invalid virtual link pulled.", title1, virtualLinks.get(0).getTitle());
	}
}