package com.dotmarketing.portlets.virtuallinks.business;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.liferay.portal.model.User;

/**
 * Executes simple tests scenarios for the {@link VirtualLinkAPI} class.
 * 
 * @author Jose Castro
 * @version 4.1.0
 * @since Apr 13, 2017
 *
 */
public class VirtualLinkAPITest extends IntegrationTestBase {

	@BeforeClass
	public static void prepare() throws Exception {
		// Setting the web app environment
		IntegrationTestInitService.getInstance().init();
	}

	@Test
	public void getVirtualLinksByTitleAndUrl() throws DotDataException, DotSecurityException {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final HostAPI siteAPI = APILocator.getHostAPI();
		final User systemUser = APILocator.getUserAPI().getSystemUser();
		final String siteName = "demo.dotcms.com";
		// Part of the Vanity URL title
		final String title = "loca";
		final Host site = siteAPI.findByName(siteName, systemUser, Boolean.FALSE);

		// Total of 4 Vanity URLs
		List<VirtualLink> vaniryUrls = virtualLinkAPI.getVirtualLinks(StringUtils.EMPTY,
				site.getHostname() + VirtualLinkAPI.URL_SEPARATOR + "/", VirtualLinkAPI.OrderBy.TITLE);
		assertEquals(4, vaniryUrls.size());

		// Only 1 Vanity URL with title "Location"
		vaniryUrls = virtualLinkAPI.getVirtualLinks(title, site.getHostname() + VirtualLinkAPI.URL_SEPARATOR + "/",
				VirtualLinkAPI.OrderBy.TITLE);
		assertEquals(1, vaniryUrls.size());
	}

	@Test
	public void getVirtualLinksByTitleAndSiteList() throws DotDataException, DotSecurityException {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final HostAPI siteAPI = APILocator.getHostAPI();
		final User systemUser = APILocator.getUserAPI().getSystemUser();
		final String siteName = "demo.dotcms.com";
		// Part of the Vanity URL title
		final String title = "loca";
		final Host site = siteAPI.findByName(siteName, systemUser, Boolean.FALSE);

		// Only 2 Vanity URLs on demo.dotcms.com
		List<VirtualLink> vanityUrls = virtualLinkAPI.getVirtualLinks(StringUtils.EMPTY, list(site),
				VirtualLinkAPI.OrderBy.TITLE);
		assertEquals(2, vanityUrls.size());

		// Only 1 Vanity URL with title "Location"
		vanityUrls = virtualLinkAPI.getVirtualLinks(title, list(site), VirtualLinkAPI.OrderBy.TITLE);
		assertEquals(1, vanityUrls.size());
	}

	@Test
	public void getHostVirtualLinks() throws DotDataException, DotSecurityException {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final HostAPI siteAPI = APILocator.getHostAPI();
		final User systemUser = APILocator.getUserAPI().getSystemUser();
		final String siteName = "demo.dotcms.com";
		final Host site = siteAPI.findByName(siteName, systemUser, Boolean.FALSE);

		// There are only 2 Vanity URLs for demo.dotcms.com
		List<VirtualLink> vanityUrls = virtualLinkAPI.getHostVirtualLinks(site);
		assertEquals(2, vanityUrls.size());
	}

	@Test
	public void checkListForCreateVirtualLinkspermission() throws DotDataException, DotSecurityException {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final HostAPI siteAPI = APILocator.getHostAPI();
		final List<User> limitedUsers = APILocator.getUserAPI().getUsersByNameOrEmail("chris@dotcms.com", 1, 1);
		final User limitedUser = limitedUsers.get(0);
		final String siteName = "demo.dotcms.com";
		final Host site = siteAPI.findByName(siteName, limitedUser, Boolean.FALSE);
		long timeInMills = System.currentTimeMillis();
		long timeInMills2 = timeInMills - 5;

		final VirtualLink vanityUrl1 = virtualLinkAPI.create("Test Vanity URL " + timeInMills,
				"/test-url-" + timeInMills, "/index", true, site, limitedUser);
		final VirtualLink vanityUrl2 = virtualLinkAPI.create("Test Vanity URL " + timeInMills2,
				"/test-url-" + timeInMills2, "/index", true, site, limitedUser);
		final List<VirtualLink> vanityUrls = list(vanityUrl1, vanityUrl2);
		final List<VirtualLink> permissionedVanityUrls = virtualLinkAPI
				.checkListForCreateVirtualLinkspermission(vanityUrls, limitedUser);

		// Limited user cannot create Vanity URLs
		assertEquals(0, permissionedVanityUrls.size());
	}

	@Test
	public void checkVirtualLinkForEditPermissions() throws DotDataException, DotSecurityException {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final HostAPI siteAPI = APILocator.getHostAPI();
		final List<User> limitedUsers = APILocator.getUserAPI().getUsersByNameOrEmail("chris@dotcms.com", 1, 1);
		final User limitedUser = limitedUsers.get(0);
		final String siteName = "demo.dotcms.com";
		final Host site = siteAPI.findByName(siteName, limitedUser, Boolean.FALSE);
		long timeInMills = System.currentTimeMillis();

		final VirtualLink vanityUrl = virtualLinkAPI.create("Test Vanity URL " + timeInMills,
				"/test-url-" + timeInMills, "/index", true, site, limitedUser);
		final VirtualLink permissionedVanityUrl = virtualLinkAPI.checkVirtualLinkForEditPermissions(vanityUrl,
				limitedUser);

		// Limited user cannot edit Vanity URLs
		assertNull(String.format("Limited user [%s] cannot edit Vanity URL in [%s]", limitedUser.getUserId(),
				site.getHostname()), permissionedVanityUrl);
	}

	@Test
	public void getVirtualLinksByURI() {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final String uri = "/about-us/locations/";
		final List<VirtualLink> vanityUrls = virtualLinkAPI.getVirtualLinksByURI(uri);

		// There's only 1 Vanity URL pointing to "/about-us/locations/"
		assertEquals(1, vanityUrls.size());
	}

	@Test
	public void getIncomingVirtualLinks() {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final String uri = "/about-us/locations/";
		final List<VirtualLink> incomingVanityUrls = virtualLinkAPI.getIncomingVirtualLinks(uri);

		// There's only 1 Vanity URL pointing to "/about-us/locations/"
		assertEquals(1, incomingVanityUrls.size());
	}

	@Test
	public void getVirtualLinkByURL() throws Exception {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final HostAPI siteAPI = APILocator.getHostAPI();
		final User systemUser = APILocator.getUserAPI().getSystemUser();
		final String siteName = "demo.dotcms.com";
		final String url = "/LOCatIOns";

		final Host site = siteAPI.findByName(siteName, systemUser, Boolean.FALSE);
		final String completeUrl = site.getHostname() + VirtualLinkAPI.URL_SEPARATOR + url;

		// Testing case insensitivity in URLs
		VirtualLink vanityUrl = virtualLinkAPI.getVirtualLinkByURL(completeUrl);
		assertNotNull("A Vanity URL object should have been returned. Please check case insensitivity.",
				vanityUrl.getUrl());
	}

	@Test
	public void getActiveVirtualLinks() {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final List<VirtualLink> vanityUrls = virtualLinkAPI.getActiveVirtualLinks();

		// There are 4 active Vanity URLs
		assertEquals(4, vanityUrls.size());
	}

	/*@Test
	public void getVirtualLink() {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		// Inode corresponding to "demo.dotcms.com:/locations"
		final String inode = "868c07ca-c2af-4668-8fe8-75d5b48bd6a5";
		final VirtualLink vanityUrl = virtualLinkAPI.getVirtualLink(inode);

		// There's 1 Vanity URL
		assertNotNull(String.format("There must be Vanity URL associated to Inode=[%s]", inode), vanityUrl);
	}*/

	@Test
	public void getVirtualLinksByConditionAndOrderBy() {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final String condition = "News";
		final List<VirtualLink> vanityUrls = virtualLinkAPI.getVirtualLinks(condition, "title");

		// There's only 1 Vanity URL whose title contains the condition: "News"
		assertEquals(1, vanityUrls.size());
	}

	@Test
	public void saveAndDelete() throws DotDataException, DotSecurityException {
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final HostAPI siteAPI = APILocator.getHostAPI();
		final User systemUser = APILocator.getUserAPI().getSystemUser();
		final String siteName = "demo.dotcms.com";
		final Host site = siteAPI.findByName(siteName, systemUser, Boolean.FALSE);
		long timeInMills = System.currentTimeMillis();
		final String url = "/test-url-" + timeInMills;

		final VirtualLink vanityUrl = virtualLinkAPI.create("Test Vanity URL " + timeInMills, url, "/index", true, site,
				systemUser);
		virtualLinkAPI.save(vanityUrl, systemUser);
		final String completeUrl = site.getHostname() + VirtualLinkAPI.URL_SEPARATOR + url;
		VirtualLink vanityUrlFromDb = virtualLinkAPI.getVirtualLinkByURL(completeUrl);

		// Verify that the Vanity URL was saved correctly
		assertEquals(completeUrl, vanityUrlFromDb.getUrl());
		// Clean up
		virtualLinkAPI.delete(vanityUrlFromDb, systemUser);
	}

}
