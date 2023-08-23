package com.dotcms.rest.api.v1.apps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.rest.api.v1.apps.view.SiteView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

public class SiteViewPaginatorIntegrationTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Here we intend to test a few things mainly:
     *     No dupe sites are returned
     *     Counts accuracy
     *     Finally we test that configured sites are the ones returned
     * Given scenario:   We create two sites, one with a configurations and one without
     * Expected result: We expect the paginator to be able to separate the sites with configurations
     *                  from the ones without. Also, we expect the counts to be accurate.
     *                  And Finally we do not expect dupes specially for the System Host
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void Site_Paginator_Accuracy_Test() throws DotDataException, DotSecurityException {
        final User user = APILocator.systemUser();
        final PermissionAPI permissionAPI = APILocator.getPermissionAPI();
        final HostAPI hostAPI = APILocator.getHostAPI();

        final Map<String, Map<String, List<String>>> warningsBySite = Map.of();

        //Two fictional sites with configurations
        final Host host1 = new SiteDataGen().nextPersisted(true);
        final Host host2 = new SiteDataGen().nextPersisted(true);
        //non configured site
        final String nonConfiguredSiteName = "non-configured-"+System.currentTimeMillis();
        final Host host3 = new SiteDataGen().name(nonConfiguredSiteName).nextPersisted(true);

        final Set<String> sitesWithConfigurations = Set.of(host1.getIdentifier(), host2.getIdentifier(), Host.SYSTEM_HOST);

        final SiteViewPaginator siteViewPaginator = new SiteViewPaginator(
                () -> sitesWithConfigurations, ()-> warningsBySite, hostAPI, permissionAPI);

        final List<String> hostIdentifiers = siteViewPaginator.getHostIdentifiers(
                user, null);

        assertTrue(hostIdentifiers.contains(host1.getIdentifier()));
        assertTrue(hostIdentifiers.contains(host2.getIdentifier()));
        //Paginator deals with system host in lower case, but we only expect it to have once
        assertEquals(1,hostIdentifiers.stream().filter(Host.SYSTEM_HOST::equalsIgnoreCase).count());

        final PaginatedArrayList<SiteView> items = siteViewPaginator.getItems(user, null, 10000, 0,
                null, OrderDirection.DESC, null);

        assertNotNull(items);
        final List<SiteView> configured = items.stream().filter(SiteView::isConfigured)
                .collect(Collectors.toList());

        //Test Total results accuracy and no dupes
        assertEquals(items.getTotalResults(), new LinkedHashSet<>(hostIdentifiers).size());

        assertEquals(sitesWithConfigurations.size(),configured.size());

        configured.forEach(siteView -> {
            assertTrue(sitesWithConfigurations.contains(siteView.getId()));
        });

        //The returned list of none non-configured sites should only contain the one we created
        final List<SiteView> nonConfigured = items.stream().filter(siteView -> !siteView.isConfigured())
                .collect(Collectors.toList());

        assertEquals(1,nonConfigured.stream().filter(siteView -> siteView.getId().equals(host3.getIdentifier())).count());

    }


}
