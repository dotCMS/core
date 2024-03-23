package com.dotcms.util.pagination;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.rest.api.v1.apps.SiteViewPaginator;
import com.dotcms.rest.api.v1.apps.view.SiteView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Ordering;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class SiteViewPaginatorUnitTest {

    private static final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    @Test
    public void Test_Get_Items_Page_Size()
            throws DotSecurityException, DotDataException {

        final int max = alphabet.length - 1;
        final User user = mockAdminUser();
        final List<String> allSites = mockAllSitesIdentifiers(max);
        final Set<String> sitesWithIntegrations = mockSitesWithConfigurations(allSites, 10);
        sitesWithIntegrations.forEach(System.out::println);
        final HostAPI hostAPI = mock(HostAPI.class);
        final long time = System.currentTimeMillis();
        int i = 0;
        final List<Host> hosts = new ArrayList<>();
        for(final String identifier:allSites){
            final Host host;
            if(Host.SYSTEM_HOST.equals(identifier)){
                host = mockSite(identifier, "System Host");
                when(hostAPI.find(eq(identifier),any(User.class), anyBoolean())).thenReturn(host);
                when(hostAPI.findSystemHost()).thenReturn(host);
            } else {
                final String name = String.format("%s%d",alphabet[i++],time);
                host = mockSite(identifier, name);
            }
            when(hostAPI.find(eq(identifier),any(User.class), anyBoolean())).thenReturn(host);
            hosts.add(host);
        }

        when(hostAPI.findAllFromCache(any(User.class),anyBoolean())).thenReturn(hosts);

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        when(permissionAPI.doesUserHavePermission(any(Host.class),anyInt(),any(User.class))).thenReturn(true);

        final Supplier<Set<String>> configuredSitesSupplier = () -> sitesWithIntegrations;
        final Supplier<Map<String, Map<String, List<String>>>> warningsBySiteSupplier = ImmutableBiMap::of;
        final SiteViewPaginator paginator = new SiteViewPaginator(configuredSitesSupplier, warningsBySiteSupplier ,hostAPI, permissionAPI);
        final int limit = sitesWithIntegrations.size();
        final PaginatedArrayList<SiteView> items = paginator
                .getItems(user, null, limit, 0, null, null, emptyMap());

        Assert.assertNotNull(items);
        Assert.assertFalse(items.isEmpty());
        Assert.assertEquals(items.get(0).getId(), Host.SYSTEM_HOST);
        Assert.assertEquals(items.size(), limit);
        for(int j=0; j < limit; j++){
            Assert.assertTrue(items.get(j).isConfigured());
        }
    }

    @Test
    public void Test_Get_Items_Sorted_Pages()
            throws DotSecurityException, DotDataException {
        final User user = mockAdminUser();
        final int maxConfigured = 6; //Only the first page is expected to bring back configured items.
        final List<String> allSites = mockAllSitesIdentifiers(alphabet.length - 1);
        final HostAPI hostAPI = mock(HostAPI.class);
        final long time = System.currentTimeMillis();
        int i = 0;
        final List<Host> hosts = new ArrayList<>();
        for(final String identifier:allSites){
            final Host host;
            final String name;
            if(Host.SYSTEM_HOST.equals(identifier)){
               name = "System Host";
               host = mockSite(identifier, name);
               when(hostAPI.find(eq(identifier),any(User.class), anyBoolean())).thenReturn(host);
               when(hostAPI.findSystemHost()).thenReturn(host);
            } else {
               name = String.format("%s%d",alphabet[i++],time);
               host = mockSite(identifier, name);
            }

            when(hostAPI.find(eq(identifier),any(User.class), anyBoolean())).thenReturn(host);
            hosts.add(host);
        }

        final Set<String> sitesWithIntegrations = mockSitesWithConfigurations(allSites, maxConfigured);

        final PermissionAPI permissionAPI = mock(PermissionAPI.class);
        when(permissionAPI.doesUserHavePermission(any(Host.class),anyInt(),any(User.class))).thenReturn(true);
        when(hostAPI.findAllFromCache(any(User.class),anyBoolean())).thenReturn(hosts);

        final Supplier<Set<String>> configuredSitesSupplier = () -> sitesWithIntegrations;
        final Supplier<Map<String, Map<String, List<String>>>> warningsBySiteSupplier = ImmutableBiMap::of;
        final SiteViewPaginator paginator = new SiteViewPaginator(configuredSitesSupplier, warningsBySiteSupplier, hostAPI, permissionAPI);

        //First batch of 6.
        int limit = sitesWithIntegrations.size();
        final PaginatedArrayList<SiteView> itemsPage1 = paginator
                .getItems(user, null, limit, 0, null, null, emptyMap());

        Assert.assertNotNull(itemsPage1);
        Assert.assertFalse(itemsPage1.isEmpty());
        Assert.assertEquals(Host.SYSTEM_HOST,itemsPage1.get(0).getId());
        itemsPage1.remove(0);

        //Requesting first page with all configured items.
        final List<String> pageNamesPage1 = itemsPage1.stream().map(SiteView::getName).collect(Collectors.toList());
        Assert.assertTrue(Ordering.<String> natural().isOrdered(pageNamesPage1));

        //Then the rest.
        final PaginatedArrayList<SiteView> itemsPage2 = paginator
                .getItems(user, null, 100, limit + 1 , null, null, emptyMap());

        final List<String> pageNamesPage2 = itemsPage2.stream().map(SiteView::getName).collect(Collectors.toList());
        Assert.assertTrue(Ordering.<String> natural().isOrdered(pageNamesPage2));

        //Test a page with both mixed configured and non-configured items are sorted.
        limit = sitesWithIntegrations.size() + 2;
        final PaginatedArrayList<SiteView> itemsPageMixed = paginator
                .getItems(user, null, limit, 0, null, null, emptyMap());

        final List<String> pageNamesConfiguredItemsPage = itemsPageMixed.stream()
                .filter(siteView -> !Host.SYSTEM_HOST.equals(siteView.getId()))
                .filter(SiteView::isConfigured).map(SiteView::getName).collect(Collectors.toList());
        Assert.assertTrue(Ordering.<String>natural().isOrdered(pageNamesConfiguredItemsPage));

        final List<String> pageNamesNonConfiguredItemsPage = itemsPageMixed.stream()
                .filter(siteView -> !Host.SYSTEM_HOST.equals(siteView.getId()))
                .filter(siteView -> !siteView.isConfigured()).map(SiteView::getName)
                .collect(Collectors.toList());
        Assert.assertTrue(Ordering.<String>natural().isOrdered(pageNamesNonConfiguredItemsPage));
    }

    private Host mockSite(final String identifier,final String name) {
        final Host host = mock(Host.class);
        when(host.getHostname()).thenReturn(name);
        when(host.getName()).thenReturn(name);
        when(host.getInode()).thenReturn(UUIDUtil.uuid());
        when(host.getIdentifier()).thenReturn(identifier);
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

    private List<String> mockAllSitesIdentifiers(final int allSitesNumber){
        final List<String> allSites = new LinkedList<>();
        //Include System host in the first position
        allSites.add(0, Host.SYSTEM_HOST);
        for(int i=0; i<= allSitesNumber; i++){
            allSites.add(""+i);
        }
        return allSites;
    }

    private Set<String> mockSitesWithConfigurations(final List<String> allSites, final int high){
        if( high > allSites.size()){
           throw new IllegalArgumentException("bound must be less or equal to allSites.size ");
        }
        final Random random = new Random();
        final int low = 1;

        final List<String> sitesWithConfigurations = new LinkedList<>();
        //Always include system host in the mocked sites.
        //Add System host upfront.
        sitesWithConfigurations.add(allSites.get(0));
        for (int i=0; i <= high; i++ ){
        //Let's add random sites making sure we dont override the first position which is already taken by system host
            sitesWithConfigurations.add(allSites.get(random.nextInt(high - low) + low));
        }
        return new HashSet<>(sitesWithConfigurations);
    }

}
