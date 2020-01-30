package com.dotcms.rest.api.v1.secret;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.rest.api.v1.secret.view.SiteView;
import com.dotcms.security.secret.ServiceDescriptor;
import com.dotcms.security.secret.ServiceIntegrationAPI;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SiteViewPaginatorTest {

    @Before
    public void init() {

    }

    @Test
    public void Test_Sort_Asc_by_Name_Then_Sort_Desc()
            throws DotSecurityException, DotDataException {

        final List<Host> sitesWithIntegrations = mockSitesWithIntegrations();

        final ServiceIntegrationAPI serviceIntegrationAPI = mock(ServiceIntegrationAPI.class);
        when(serviceIntegrationAPI.getSitesWithIntegrations(any(User.class)))
                .thenReturn(sitesWithIntegrations);
        when(serviceIntegrationAPI
                .filterSitesForService(anyString(), anyListOf(Host.class), any(User.class)))
                .thenReturn(sitesWithIntegrations);

        final HostAPI hostAPI = mock(HostAPI.class);
        final List<Host> allSites = new ArrayList<>();
        when(hostAPI.findAll(any(User.class), anyBoolean())).thenReturn(allSites);

        final SiteViewPaginator paginator = new SiteViewPaginator(serviceIntegrationAPI, hostAPI);

        final User user = mockAdminUser();

        final ServiceDescriptor serviceDescriptor = new ServiceDescriptor("test-key", "", "", "",
                false, ImmutableMap.of());

        final Map<String, Object> extraParams = ImmutableMap
                .of(SiteViewPaginator.SERVICE_DESCRIPTOR, serviceDescriptor);

        final PaginatedArrayList<SiteView> ascOrderedItems = paginator
                .getItems(user, null, 10, 0, "name", OrderDirection.ASC, extraParams);

        final List<String> alphabeticalOrder = ImmutableList
                .of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        int i = 0;
        for (final String expectedName : alphabeticalOrder) {
            final SiteView siteView = ascOrderedItems.get(i++);
            Assert.assertEquals(expectedName, siteView.getName());
        }

        final PaginatedArrayList<SiteView> descOrderedItems = paginator
                .getItems(user, null, 10, 0, "name", OrderDirection.DESC, extraParams);

        final List<String> alphabeticReversedOrder = ImmutableList
                .of("j", "i", "h", "g", "f", "e", "d", "c", "b", "a");
        i = 0;
        for (final String expectedName : alphabeticReversedOrder) {
            final SiteView siteView = descOrderedItems.get(i++);
            Assert.assertEquals(expectedName, siteView.getName());
        }

    }

    @Test


    public void Test_Sort_Asc_by_Integrations_Then_Sort_Desc()
            throws DotSecurityException, DotDataException {

        final List<Host> allSites = mockAllSites();
        final List<Host> sitesWithIntegrations = mockSitesWithIntegrations();

        final Map<String, Host> integratedSitesById = sitesWithIntegrations.stream().collect(
                Collectors.toMap(Host::getIdentifier, Function.identity()));

        final ServiceIntegrationAPI serviceIntegrationAPI = mock(ServiceIntegrationAPI.class);
        when(serviceIntegrationAPI.getSitesWithIntegrations(any(User.class)))
                .thenReturn(sitesWithIntegrations);

        when(serviceIntegrationAPI
                .filterSitesForService(anyString(), anyListOf(Host.class), any(User.class)))
                .thenReturn(sitesWithIntegrations);

        final HostAPI hostAPI = mock(HostAPI.class);

        when(hostAPI.findAll(any(User.class), anyBoolean())).thenReturn(allSites);

        final SiteViewPaginator paginator = new SiteViewPaginator(serviceIntegrationAPI, hostAPI);

        final User user = mockAdminUser();

        final ServiceDescriptor serviceDescriptor = new ServiceDescriptor("test-key", "", "", "",
                false, ImmutableMap.of());

        final Map<String, Object> extraParams = ImmutableMap
                .of(SiteViewPaginator.SERVICE_DESCRIPTOR, serviceDescriptor);

        final PaginatedArrayList<SiteView> ascOrderedItems = paginator
                .getItems(user, null, 10, 0, "integrated", OrderDirection.ASC, extraParams);

        for (final Host host : sitesWithIntegrations) {

        }

    }


    private Host mockSite(final String name) {
        final Host host = mock(Host.class);
        when(host.getHostname()).thenReturn(name);
        when(host.getInode()).thenReturn(UUIDUtil.uuid());
        when(host.getIdentifier()).thenReturn(UUIDUtil.uuid());
        return host;
    }

    private User mockAdminUser() {
        final User adminUser = mock(User.class);
        when(adminUser.getUserId()).thenReturn("dotcms.org.1");
        when(adminUser.getEmailAddress()).thenReturn("admin@dotcms.com");
        when(adminUser.getFirstName()).thenReturn("Admin");
        when(adminUser.getLastName()).thenReturn("User");
        return adminUser;
    }

    private List<Host> mockSitesWithIntegrations(){
        return ImmutableList.of(
                mockSite("j"),
                mockSite("g"),
                mockSite("b"),
                mockSite("c"),
                mockSite("d"),
                mockSite("a"),
                mockSite("e"),
                mockSite("h"),
                mockSite("i"),
                mockSite("f"),
                mockSite("k")
        );
    }

    private List<Host> mockAllSites(){
        final List <Host> allHost = new ArrayList<>(mockSitesWithIntegrations());
        for(int i=0; i<= 30; i++){
            final String name = RandomStringUtils.randomAlphanumeric(20);
            allHost.add(mockSite(name));
        }
        return allHost;
    }

}
