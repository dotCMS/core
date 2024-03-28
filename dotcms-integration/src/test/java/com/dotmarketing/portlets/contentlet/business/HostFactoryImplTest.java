package com.dotmarketing.portlets.contentlet.business;
import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.*;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.dotmarketing.portlets.contentlet.business.HostFactoryImpl.SITE_IS_LIVE_OR_STOPPED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class HostFactoryImplTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }
    /**
     * Method to test: {@link HostFactoryImpl#findLiveAndStopped(String, int, int, boolean, User, boolean)}
     * Given Scenario: Create two tests, one line and the other stopped. Compare the amount of the retrieve list.
     * ExpectedResult: When compared to the initial stopped Sites count, after creating the stopped Site,
     *      *     the count must be at least 2. After adding the new live site, the total count difference
     *      *     must be at least 3. Archived sites should not be visible.
     *
     */
    @Test
    public void test_findLiveAndStopped_shouldOnlyRetunLiveAndStoppedSites() throws DotDataException, DotSecurityException {
        // Initialization
        final int limit = -1;
        final int offset = 0;
        final HostFactoryImpl hostFactory = new HostFactoryImpl();
        final long systemMilis = System.currentTimeMillis();

        //live site obj
        final Host LiveTestSite = new SiteDataGen().name("liveHost"+systemMilis).nextPersisted(true);

        //start validations of the live site
        //sites lists
        final List<Host> allSites = hostFactory.findAll(limit, offset,"name");
        final Optional<List<Host>> listStoppedLive = hostFactory.findLiveAndStopped("", limit, offset, false, APILocator.systemUser(), false);

        //asserts validations
        assertTrue( "Live test is not contained in all sites list of length "+allSites.size(), allSites.contains(LiveTestSite));
        if (listStoppedLive.isPresent()) {
            assertTrue( "Live test is not contained in live/stopped list sites of length "+listStoppedLive.get().size(), listStoppedLive.get().contains(LiveTestSite));
        } else {
            assertTrue(listStoppedLive.isPresent());
        }

        //stopped site obj
        final Host stoppedTestSite = new SiteDataGen().name("stoppedHost"+systemMilis).nextPersisted(false);

        //sites lists
        final List<Host> allHostsList =  hostFactory.findAll(limit, offset,"name");
        final Optional<List<Host>> hostsList =  hostFactory.findLiveAndStopped("", limit, offset, false, APILocator.systemUser(), false);


        //validations
        assertTrue( "Stopped test is not contained in all sites list of length "+allHostsList.size(), allHostsList.contains(stoppedTestSite));

        if (hostsList.isPresent()) {
            assertTrue( "Stopped test is not contained in live/stopped list sites of length "+hostsList.get().size(), hostsList.get().contains(stoppedTestSite));
        } else {
            assertTrue(hostsList.isPresent());
        }

    }

    /**
     * Method to test: {@link HostFactoryImpl#search(String, String, boolean, int, int, User, boolean)}
     * Given Scenario: Create many (20+) sites that have the same text in them
     * example1.test.com, example2.test.com..., then just test.com
     * ExpectedResult: Exact matches should be at the top of the search results.
     *
     */
    @Test
    public void test_search_shouldReturnExactMatchesFirst() throws DotDataException, DotSecurityException {
        // Initialization
        final int limit = 15;
        final int offset = 0;
        final HostFactoryImpl hostFactory = new HostFactoryImpl();
        final long systemMilis = System.currentTimeMillis();

        final String baseName = "test.com";

        // generate 20 sites with the name test.com
        for (int i = 0; i < 20; i++) {
            new SiteDataGen().name("example"+i+"-"+systemMilis+"."+baseName).nextPersisted(true);
        }

        //get the site with the name test.com
        Host testSite = APILocator.getHostAPI().findByName(baseName, APILocator.systemUser(), false);

        //validate if the site is null
        //if is null create a new site with the name test.com
        if (testSite == null) {
            testSite = new SiteDataGen().name(baseName).nextPersisted(true);
        }

        // test the method search at class HostFactoryImpl where the filter is "test.com" and should return it first in list
        final Optional<List<Host>> hostsList =  hostFactory.search(baseName, SITE_IS_LIVE_OR_STOPPED,false ,limit, offset, APILocator.systemUser(), false);

        //validations
        assertTrue( "Test site is not contained in list", hostsList.get().contains(testSite));
        assertEquals("Test site is not the first in the list", testSite, hostsList.get().get(0));
    }

    /**
     * <ul>
     *     <li><b>Method to test: {@link HostFactoryImpl#search(String, String, boolean, int, int, User, boolean, List)}</li>
     *     <li><b>Given Scenario: Limited users should be able to create content types on System Host if given the proper permissions</b> </li>
     *     <li><b>Expected Result:The System host should appear in the list of host </b> </li>
     * </ul>
     * @throws Exception
     */
    @Test
    public void test_search_shouldIncludeSystemHost() throws DotDataException, DotSecurityException {

        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final HostFactoryImpl hostFactory = new HostFactoryImpl();
        //Create limited user
        final User limitedUser = new UserDataGen().nextPersisted();

        //user has no permissions over the system host
        Optional<List<Host>> hostsList =  hostFactory.search("", "cvi.live_inode IS NOT NULL",true ,15, 0, limitedUser, false, new ArrayList<>());
        assertTrue(hostsList.isPresent() && !hostsList.get().contains(APILocator.systemHost()));

        //Give Permissions Over the System Host
        final Permission permissions = new Permission(Host.class.getCanonicalName(),
                APILocator.systemHost().getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT, true);
        permissionAPI.save(permissions, APILocator.systemHost(), APILocator.getUserAPI().getSystemUser(), false);
        //method to test
        hostsList =  hostFactory.search("", "cvi.live_inode IS NOT NULL",true ,15, 0, limitedUser, false, new ArrayList<>());
        assertTrue(hostsList.isPresent() && hostsList.get().size() > 0);
        //now the user has permissions over the system host
        assertTrue(hostsList.get().contains(APILocator.systemHost()));
    }
}














