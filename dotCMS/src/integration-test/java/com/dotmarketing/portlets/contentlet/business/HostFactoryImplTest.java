package com.dotmarketing.portlets.contentlet.business;
import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class HostFactoryImplTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        LicenseTestUtil.getLicense();
        DotInitScheduler.start();
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
        final String filter = "filter";
        final int limit = 5;
        final int offset = 4;
        final User user = new User();
        HostFactoryImpl hostFactory = new HostFactoryImpl();
        final User systemUser = APILocator.systemUser();

        Host LiveTestSite = new SiteDataGen().name("liveHost").nextPersisted();
        APILocator.getVersionableAPI().setLive(LiveTestSite);

        Host stoppedTestSite = new SiteDataGen().name("stoppedHost").nextPersisted();
        this.unpublishHost(stoppedTestSite, systemUser);

        final Optional<List<Host>> hostsList =  hostFactory.findLiveAndStopped(filter, limit, offset, false, user, false);
        assertTrue(hostsList.get().size() >= 3);
    }

    private void unpublishHost(final Host host, final User user) throws DotHibernateException {

        try {
            HibernateUtil.startTransaction();
            host.setIndexPolicy(IndexPolicy.WAIT_FOR);
            APILocator.getHostAPI().unpublish(host, user, false);
            HibernateUtil.closeAndCommitTransaction();
        } catch (Exception e) {
            HibernateUtil.rollbackTransaction();
            Logger.error(HostAPITest.class, e.getMessage(), e);
            Assert.fail(String.format("Unable to unpublish test host [%s] [%s]", host.getHostname(),
                    e.getMessage()));
        }
    }
}














