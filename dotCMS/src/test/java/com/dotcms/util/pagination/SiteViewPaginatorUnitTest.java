package com.dotcms.util.pagination;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.rest.api.v1.secret.SiteViewPaginator;
import com.dotcms.rest.api.v1.secret.view.SiteView;
import com.dotmarketing.beans.Host;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UUIDUtil;
import com.google.common.collect.Ordering;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
        final Set<String> sitesWithIntegrations = mockSitesWithIntegrations(allSites, 10);
        //System.out.println("Sites with integrations: ");
        sitesWithIntegrations.forEach(System.out::println);
        final HostAPI hostAPI = mock(HostAPI.class);
        long time = System.currentTimeMillis();
        int i = 0;
        //System.out.println("Site names: ");
        for(final String identifier:allSites){
            final Host host;
            if(Host.SYSTEM_HOST.equals(identifier)){
                host = mockSite(identifier, "System Host");
                when(hostAPI.find(eq(identifier),any(User.class), anyBoolean())).thenReturn(host);
            } else {
                final String name = alphabet[i++] + "" +  time;
                System.out.println(name);
                host = mockSite(identifier, name);
            }
            when(hostAPI.find(eq(identifier),any(User.class), anyBoolean())).thenReturn(host);
        }

        final List<ContentletSearch> mockedSearch = mockSearchResults(allSites);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        when(contentletAPI.searchIndex(anyString(), anyInt(), anyInt(), eq("title"), any(User.class), anyBoolean())).thenReturn(mockedSearch);
        final Supplier<Set<String>> configuredSitesSupplier = () -> sitesWithIntegrations;

        final SiteViewPaginator paginator = new SiteViewPaginator(configuredSitesSupplier, hostAPI, contentletAPI);
        final int limit = sitesWithIntegrations.size();
        final PaginatedArrayList<SiteView> items = paginator
                .getItems(user, null, limit, 0, null, null, Collections.emptyMap());

        Assert.assertNotNull(items);
        Assert.assertFalse(items.isEmpty());
        Assert.assertEquals(items.get(0).getId(), Host.SYSTEM_HOST);
        Assert.assertEquals(items.size(), limit);
        //First item is'nt necessarily configured. So we start counting from 1.
        for(int j=1; j < limit; j++){
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
        final List<String> allSitesSortedIdentifiers = new LinkedList<>();
        final long time = System.currentTimeMillis();
        //final Map<String,String> debugInfo = new TreeMap<>();
        int i = 0;
        //System.out.println("Site names: ");
        for(final String identifier:allSites){
            final Host host;
            final String name;
            if(Host.SYSTEM_HOST.equals(identifier)){
               name = "System Host";
               host = mockSite(identifier, name);
               when(hostAPI.find(eq(identifier),any(User.class), anyBoolean())).thenReturn(host);
            } else {
               name = alphabet[i++] + "" +  time;
               host = mockSite(identifier, name);
            }
            //System.out.println(identifier + ":" + name);
            //debugInfo.put(identifier,name);
            when(hostAPI.find(eq(identifier),any(User.class), anyBoolean())).thenReturn(host);
            allSitesSortedIdentifiers.add(identifier);
        }

        final Set<String> sitesWithIntegrations = mockSitesWithIntegrations(allSites, maxConfigured);
        //System.out.println("Sites with integrations: ");
        //for (final String configurationIdentifier : sitesWithIntegrations) {
        //    System.out.println( configurationIdentifier + ":"  + debugInfo.get(configurationIdentifier));
        //}

        final List<ContentletSearch> mockedSearch = mockSearchResults(allSitesSortedIdentifiers);
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        when(contentletAPI.searchIndex(anyString(), anyInt(), anyInt(), eq("title"), any(User.class), anyBoolean())).thenReturn(mockedSearch);
        final Supplier<Set<String>> configuredSitesSupplier = () -> sitesWithIntegrations;

        final SiteViewPaginator paginator = new SiteViewPaginator(configuredSitesSupplier, hostAPI, contentletAPI);

        //First batch of 6.
        int limit = sitesWithIntegrations.size();
        final PaginatedArrayList<SiteView> itemsPage1 = paginator
                .getItems(user, null, limit, 0, null, null, Collections.emptyMap());

        Assert.assertNotNull(itemsPage1);
        Assert.assertFalse(itemsPage1.isEmpty());
        Assert.assertEquals(Host.SYSTEM_HOST,itemsPage1.get(0).getId());
        itemsPage1.remove(0);

        //Requesting first page with all configured items.
        final List<String> pageNamesPage1 = itemsPage1.stream().map(SiteView::getName).collect(Collectors.toList());
        Assert.assertTrue(Ordering.<String> natural().isOrdered(pageNamesPage1));

        //Then the rest.
        final PaginatedArrayList<SiteView> itemsPage2 = paginator
                .getItems(user, null, 100, limit + 1 , null, null, Collections.emptyMap());

        final List<String> pageNamesPage2 = itemsPage2.stream().map(SiteView::getName).collect(Collectors.toList());
        Assert.assertTrue(Ordering.<String> natural().isOrdered(pageNamesPage2));

        //Test a page with both mixed configured and non-configured items are sorted.
        limit = sitesWithIntegrations.size() + 2;
        final PaginatedArrayList<SiteView> itemsPageMixed = paginator
                .getItems(user, null, limit, 0, null, null, Collections.emptyMap());

        final List<String> pageNamesConfiguredItemsPage = itemsPage2.stream().filter(SiteView::isConfigured).map(SiteView::getName).collect(Collectors.toList());
        Assert.assertTrue(Ordering.<String> natural().isOrdered(pageNamesConfiguredItemsPage));

        final List<String> pageNamesNonConfiguredItemsPage = itemsPage2.stream().filter(siteView -> !siteView.isConfigured()).map(SiteView::getName).collect(Collectors.toList());
        Assert.assertTrue(Ordering.<String> natural().isOrdered(pageNamesNonConfiguredItemsPage));

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

    private List<String> mockAllSitesIdentifiers(int allSitesNumber){
        final List<String> allSites = new LinkedList<>();
        allSites.add(0, Host.SYSTEM_HOST);
        for(int i=0; i<= allSitesNumber; i++){
            allSites.add(""+i);
        }
        return allSites;
    }

    private Set<String> mockSitesWithIntegrations(final List<String> allSites, final int bound){
        if( bound > allSites.size()){
           throw new IllegalArgumentException("bound must be less or equal to allSites.size ");
        }
        final Random random = new Random();
        final List<String> sitesWithIntegrations = new LinkedList<>();
        for (int i=0; i <= bound; i++ ){
            sitesWithIntegrations.add(allSites.get(random.nextInt(bound)));
        }
        return new HashSet<>(sitesWithIntegrations);
    }

    private List<ContentletSearch> mockSearchResults(List<String> allSites){
        final List<ContentletSearch> mocks = new ArrayList<>(allSites.size());
        for (final String identifier : allSites) {
            final ContentletSearch search = mock(ContentletSearch.class);
            when(search.getIdentifier()).thenReturn(identifier);
            mocks.add(search);
        }
        return mocks;
    }

}
