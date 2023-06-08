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
    public void test_findLiveAndStopped_shouldOnlyRetunLiveAndStoppedSites() {
        // Initialization
        final int limit = 100;
        final int offset = 0;
        final HostFactoryImpl hostFactory = new HostFactoryImpl();
        final long systemMilis = System.currentTimeMillis();

        final Host LiveTestSite = new SiteDataGen().name("liveHost"+systemMilis).nextPersisted();

        final Host stoppedTestSite = new SiteDataGen().name("stoppedHost"+systemMilis).nextPersisted(false);

        final Optional<List<Host>> hostsList =  hostFactory.findLiveAndStopped("", limit, offset, false, APILocator.systemUser(), false);

        assertTrue(hostsList.get().contains(LiveTestSite) && hostsList.get().contains(stoppedTestSite));
    }
}














