package com.dotmarketing.portlets.virtuallinks.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;

/**
 * Executes simple tests scenarios for the {@link VirtualLinkAPI} class.
 * 
 * @author Jose Castro
 * @version 4.1.0
 * @since Apr 13, 2017
 *
 */
public class VirtualLinkAPITest extends IntegrationTestBase {

	private static Collection<VirtualLink> activeVirtualLinks;
	private static Collection<VirtualLink> nonActiveVirtualLinks;
	private static Host site;
	private static User systemUser;

	@BeforeClass
	public static void prepare() throws Exception {
		// Setting the web app environment
		IntegrationTestInitService.getInstance().init();

		//Create some virtual links for testing
		activeVirtualLinks = new ArrayList<>();
		nonActiveVirtualLinks = new ArrayList<>();

		//Init APIs and test values
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final HostAPI siteAPI = APILocator.getHostAPI();
		systemUser = APILocator.getUserAPI().getSystemUser();

		final String siteName = "demo.dotcms.com";
		site = siteAPI.findByName(siteName, systemUser, Boolean.FALSE);

		//Build the test collection
		activeVirtualLinks.add(
				createTestVirtualLink("Test1 Vanity URL ", "/test1-url-", true, systemUser, site, virtualLinkAPI)
		);
		activeVirtualLinks.add(
				createTestVirtualLink("Test2 Vanity URL ", "/test2-url-", true, systemUser, site, virtualLinkAPI)
		);
		nonActiveVirtualLinks.add(
				createTestVirtualLink("Test3 Vanity URL ", "/test3-url-", false, systemUser, site, virtualLinkAPI)
		);
		nonActiveVirtualLinks.add(
				createTestVirtualLink("Test4 Vanity URL ", "/test4-url-", false, systemUser, site, virtualLinkAPI)
		);
		activeVirtualLinks.add(
				createTestVirtualLink("Case sensitive test value1 Vanity URL", "/cAsE-senSITIve-TEST1", true, systemUser, site, virtualLinkAPI, false)
		);
		activeVirtualLinks.add(
				createTestVirtualLink("Search test value1 Vanity URL ", "/search-test-value1-url-", true, systemUser, site, virtualLinkAPI)
		);
		activeVirtualLinks.add(
				createTestVirtualLink("Search test value2 Vanity URL ", "/search-test-value2-url-", true, systemUser, site, virtualLinkAPI)
		);
		nonActiveVirtualLinks.add(
				createTestVirtualLink("Search test value3 Vanity URL ", "/search-test-value3-url-", false, systemUser, site, virtualLinkAPI)
		);

	}

	@Test
	public void getVirtualLinksByTitleAndUrl() throws DotDataException, DotSecurityException {

		//Initialize test data
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

		// Get all the ACTIVE vanity urls on this host
		List<VirtualLink> currentVirtualLinks = virtualLinkAPI.getVirtualLinks(StringUtils.EMPTY,
				site.getHostname() + VirtualLinkAPI.URL_SEPARATOR + "/", VirtualLinkAPI.OrderBy.TITLE);
		assertNotNull(currentVirtualLinks);
		assertFalse(currentVirtualLinks.isEmpty());

		// Apply a search using the title
		String title = "Search test value";
		//Return any ACTIVE virtual link that contains the words "Search test value"
		currentVirtualLinks = virtualLinkAPI.getVirtualLinks(title, site.getHostname() + VirtualLinkAPI.URL_SEPARATOR + "/",
				VirtualLinkAPI.OrderBy.TITLE);
		assertEquals(2, currentVirtualLinks.size());
	}

	@Test
	public void getVirtualLinksByTitleAndSiteList() throws DotDataException, DotSecurityException {

		//Initialize test data
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

		// Get all the ACTIVE vanity urls on the given list of hosts
		List<VirtualLink> currentVirtualLinks = virtualLinkAPI.getVirtualLinks(StringUtils.EMPTY, list(site),
				VirtualLinkAPI.OrderBy.TITLE);
		assertNotNull(currentVirtualLinks);
		assertFalse(currentVirtualLinks.isEmpty());

		// Apply a search using the title
		String title = "Search test value";
		//Return any ACTIVE virtual link that contains the words "Search test value"
		currentVirtualLinks = virtualLinkAPI.getVirtualLinks(title, list(site), VirtualLinkAPI.OrderBy.TITLE);
		assertEquals(2, currentVirtualLinks.size());
	}

	@Test
	public void getHostVirtualLinks() throws DotDataException, DotSecurityException {

		//Initialize test data
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

		// Get all the ACTIVE vanity urls on the given list of hosts
		List<VirtualLink> currentVirtualLinks = virtualLinkAPI.getVirtualLinks(StringUtils.EMPTY, list(site),
				VirtualLinkAPI.OrderBy.TITLE);
		assertNotNull(currentVirtualLinks);
		assertFalse(currentVirtualLinks.isEmpty());

		// Now we need to compare what is return by the getHostVirtualLinks method (returns ACTIVE and NOT Active virtual links)
		List<VirtualLink> vanityUrls = virtualLinkAPI.getHostVirtualLinks(site);
		assertNotNull(currentVirtualLinks);
		assertFalse(currentVirtualLinks.isEmpty());
		assertTrue(vanityUrls.size() > currentVirtualLinks.size());
	}

	@Test
	public void checkListForCreateVirtualLinkspermission() throws DotDataException, DotSecurityException {

		//Initialize test data
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final List<User> limitedUsers = APILocator.getUserAPI().getUsersByNameOrEmail("chris@dotcms.com", 1, 1);
		final User limitedUser = limitedUsers.get(0);

		long timeInMills = System.currentTimeMillis();
		long timeInMills2 = timeInMills - 5;

		final VirtualLink vanityUrl1 = virtualLinkAPI.create("Test create Vanity URL " + timeInMills,
				"/test-create-url-" + timeInMills, "/index", true, site, limitedUser);
		final VirtualLink vanityUrl2 = virtualLinkAPI.create("Test create Vanity URL " + timeInMills2,
				"/test-create-url-" + timeInMills2, "/index", true, site, limitedUser);
		final List<VirtualLink> vanityUrls = list(vanityUrl1, vanityUrl2);
		final List<VirtualLink> permissionedVanityUrls = virtualLinkAPI
				.checkListForCreateVirtualLinkspermission(vanityUrls, limitedUser);

		// Limited user cannot create Vanity URLs
		assertEquals(0, permissionedVanityUrls.size());
	}

	@Test
	public void checkVirtualLinkForEditPermissions() throws DotDataException, DotSecurityException {

		//Initialize test data
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();
		final List<User> limitedUsers = APILocator.getUserAPI().getUsersByNameOrEmail("chris@dotcms.com", 1, 1);
		final User limitedUser = limitedUsers.get(0);

		long timeInMills = System.currentTimeMillis();

		final VirtualLink vanityUrl = virtualLinkAPI.create("Test create Vanity URL " + timeInMills,
				"/test-create-url-" + timeInMills, "/index", true, site, limitedUser);
		final VirtualLink permissionedVanityUrl = virtualLinkAPI.checkVirtualLinkForEditPermissions(vanityUrl,
				limitedUser);

		// Limited user cannot edit Vanity URLs
		assertNull(String.format("Limited user [%s] cannot edit Vanity URL in [%s]", limitedUser.getUserId(),
				site.getHostname()), permissionedVanityUrl);
	}

	@Test
	public void getVirtualLinksByURI() throws DotDataException, DotSecurityException {

		//Initialize test data
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

		//First search for a virtual link for the test
		VirtualLink testVirtualLink = activeVirtualLinks.iterator().next();

		//Now we need to test the getVirtualLinksByURI and check that is returning the right data
		final List<VirtualLink> vanityUrls = virtualLinkAPI.getVirtualLinksByURI(testVirtualLink.getUri());

		// There's only 1 Vanity URL pointing to "/about-us/locations/"
		assertNotNull(vanityUrls);
		assertFalse(vanityUrls.isEmpty());
		assertEquals(1, vanityUrls.size());
		assertEquals(testVirtualLink.getInode(), vanityUrls.iterator().next().getInode());
	}

	@Test
	public void getIncomingVirtualLinks() throws DotDataException, DotSecurityException {

		//Initialize test data
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

		//First search for an ACTIVE virtual link
		VirtualLink activeVirtualLink = activeVirtualLinks.iterator().next();

		//Now we need to test the getIncomingVirtualLinks and check that is returning the right data
		List<VirtualLink> vanityUrls = virtualLinkAPI.getIncomingVirtualLinks(activeVirtualLink.getUri());

		// There's only 1 Vanity URL pointing to "/about-us/locations/"
		assertNotNull(vanityUrls);
		assertFalse(vanityUrls.isEmpty());
		assertEquals(1, vanityUrls.size());
		assertEquals(activeVirtualLink.getInode(), vanityUrls.iterator().next().getInode());

		//Now search for a NON ACTIVE virtual link
		VirtualLink nonActiveVirtualLink = nonActiveVirtualLinks.iterator().next();

		//Now we need to test the getIncomingVirtualLinks and check that is returning the right data
		vanityUrls = virtualLinkAPI.getIncomingVirtualLinks(nonActiveVirtualLink.getUri());

		//The getIncomingVirtualLinks should only work for ACTIVE virtual links
		assertNotNull(vanityUrls);
		assertTrue(vanityUrls.isEmpty());
	}

	@Test
	public void getVirtualLinkByURL() throws Exception {

		//Initialize test data
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

		final String url = "/CASe-SENSItiVE-test1";
		final String completeUrl = site.getHostname() + VirtualLinkAPI.URL_SEPARATOR + url;

		// Testing case insensitivity in URLs
		VirtualLink vanityUrl = virtualLinkAPI.getVirtualLinkByURL(completeUrl);
		assertNotNull("A Vanity URL object should have been returned. Please check case insensitivity.",
				vanityUrl.getUrl());
	}

	@Test
	public void getActiveVirtualLinks() throws DotDataException, DotSecurityException {

		//Initialize test data
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

		//And now we need search just for the active ones
		final Collection<VirtualLink> activeVanityUrls = virtualLinkAPI.getActiveVirtualLinks();

		// Verify what we found
		assertNotNull(activeVanityUrls);
		assertFalse(activeVanityUrls.isEmpty());

		//Only active VirtualLinks should be returned
		for ( VirtualLink virtualLink : activeVanityUrls ) {
			assertTrue(virtualLink.isActive());
		}
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
		final String condition = "test2-url-";
		final List<VirtualLink> vanityUrls = virtualLinkAPI.getVirtualLinks(condition, "title");

		// There's only 1 Vanity URL whose title contains the condition: "News"
		assertEquals(1, vanityUrls.size());
	}

	@Test
	public void saveAndDelete() throws DotDataException, DotSecurityException {

		//Initialize test data
		final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

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

	/**
	 * Creates based on the given values a VirtualLink object
	 *
	 * @param title
	 * @param url
	 * @param isActive
	 * @param user
	 * @param site
	 * @param virtualLinkAPI
	 * @param applyMillis
	 * @return
	 */
	private static VirtualLink createTestVirtualLink(String title, String url, Boolean isActive, User user,
													 Host site, VirtualLinkAPI virtualLinkAPI, Boolean applyMillis) {

		long timeInMills = System.currentTimeMillis();

		//Create the virtual link
		final VirtualLink vanityUrl = virtualLinkAPI.create(
				applyMillis ? title + timeInMills : title,
				applyMillis ? url + timeInMills : url,
				applyMillis ? "/index-" + timeInMills : "/index",
				isActive,
				site,
				user);

		try {
			//And save it
			virtualLinkAPI.save(vanityUrl, user);
		} catch (Exception e) {
			Logger.error(VirtualLinkAPITest.class, "Unable to create virtual link.");
		}

		return vanityUrl;
	}

	/**
	 * Creates based on the given values a VirtualLink object
	 *
	 * @param title
	 * @param url
	 * @param isActive
	 * @param user
	 * @param site
	 * @param virtualLinkAPI
	 * @return
	 */
	private static VirtualLink createTestVirtualLink(String title, String url, Boolean isActive, User user, Host site, VirtualLinkAPI virtualLinkAPI) {
		return createTestVirtualLink(title, url, isActive, user, site, virtualLinkAPI, true);
	}

	@AfterClass
	public static void cleanup() throws Exception {

		if ( null != activeVirtualLinks ) {
			if ( null != nonActiveVirtualLinks ) {
				activeVirtualLinks.addAll(nonActiveVirtualLinks);
			}

			//Init APIs and test values
			final VirtualLinkAPI virtualLinkAPI = APILocator.getVirtualLinkAPI();

			//Clean up all the created test data
			Iterator<VirtualLink> virtualLinksIterator = activeVirtualLinks.iterator();
			while ( virtualLinksIterator.hasNext() ) {
				try {
					virtualLinkAPI.delete(virtualLinksIterator.next(), systemUser);
				} catch (Exception e) {
					Logger.error(VirtualLinkAPITest.class, "Unable to delete virtual link.");
				}
			}
		}
	}

}