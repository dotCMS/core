package com.dotcms.util.pagination;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test {@link SitePaginator}
 */
public class SitePaginatorTest {

    HostAPI hostAPI;
    SitePaginator sitePaginator;

    PaginatedArrayList<Host> hosts;
    int totalRecords;

    @Before
    public void init(){
        hostAPI = mock(HostAPI.class);
        sitePaginator = new SitePaginator( hostAPI );

        totalRecords = 5;

        hosts = new PaginatedArrayList<>();
        hosts.setTotalResults( totalRecords );
        hosts.add( mock( Host.class ) );
        hosts.add( mock( Host.class ) );
        hosts.add( mock( Host.class ) );
        hosts.add( mock( Host.class ) );
        hosts.add( mock( Host.class ) );
    }

    /**
     * <b>Method to test:</b> {@link SitePaginator#getItems(User, String, int, int, String, OrderDirection, Map)} <br></br>
     * <b>Given Scenario:</b> SitePaginator is invoked with a dummy filter<br></br>
     * <b>Expected Result:</b> {@link HostAPI#search(String, boolean, int, int, User, boolean)} is expected to be called<br></br>
     */
    @Test
    public void testGetItems(){
        final String filter = "filter";
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(hostAPI.search( filter, false, limit, offset, user, false ))
                .thenReturn( hosts );

        final PaginatedArrayList<Host> items = sitePaginator.getItems(user, filter, limit, offset, null, null);

        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(hosts, items);
    }

    /**
     * <b>Method to test:</b> {@link SitePaginator#getItems(User, String, int, int, String, OrderDirection, Map)} <br></br>
     * <b>Given Scenario:</b> SitePaginator is invoked with a dummy filter to get archived items<br></br>
     * <b>Expected Result:</b> {@link HostAPI#search(String, boolean, boolean, boolean, int, int, User, boolean)} is expected to be called<br></br>
     */
    @Test
    public void testGetItemsWithArchived(){
        final String filter = "filter";
        final boolean showArchived = true;
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(hostAPI.search( filter, showArchived, true, false, limit, offset, user, false ))
                .thenReturn( hosts );

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put(SitePaginator.ARCHIVED_PARAMETER_NAME, showArchived);
        extraParams.put(SitePaginator.LIVE_PARAMETER_NAME, null);

        final PaginatedArrayList<Host> items = sitePaginator.getItems(user, filter, limit, offset, null, null, extraParams);
        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(hosts, items);
    }

    /**
     * <b>Method to test:</b> {@link SitePaginator#getItems(User, String, int, int, String, OrderDirection, Map)} <br></br>
     * <b>Given Scenario:</b> SitePaginator is invoked with a dummy filter to get stopped hosts<br></br>
     * <b>Expected Result:</b> {@link HostAPI#searchByStopped(String, boolean, boolean, int, int, User, boolean)} is expected to be called<br></br>
     */
    @Test
    public void testGetItemsWithStopped(){
        final String filter = "filter";
        final boolean showStopped = true;
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(hostAPI.searchByStopped( filter, showStopped,false, limit, offset, user, false ))
                .thenReturn( hosts );

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put(SitePaginator.ARCHIVED_PARAMETER_NAME, null);
        extraParams.put(SitePaginator.LIVE_PARAMETER_NAME, !showStopped);
        final PaginatedArrayList<Host> items = sitePaginator.getItems(user, filter, limit, offset, null, null, extraParams);

        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(hosts, items);
    }

    /**
     * <b>Method to test:</b> {@link SitePaginator#getItems(User, String, int, int, String, OrderDirection, Map)} <br></br>
     * <b>Given Scenario:</b> SitePaginator is invoked with a dummy filter to get stopped and archived hosts<br></br>
     * <b>Expected Result:</b> {@link HostAPI#search(String, boolean, boolean, boolean, int, int, User, boolean)} is expected to be called<br></br>
     */
    @Test
    public void testGetItemsWithStoppedAndArchived(){
        final String filter = "filter";
        final boolean showArchived = true;
        final boolean showStopped = true;
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(hostAPI.search( filter, showArchived, showStopped,false, limit, offset, user, false ))
                .thenReturn( hosts );

        final PaginatedArrayList<Host> items = sitePaginator.getItems(user, filter, limit, offset, null, null,
                Map.of(SitePaginator.ARCHIVED_PARAMETER_NAME, showArchived, SitePaginator.LIVE_PARAMETER_NAME, !showStopped));

        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(hosts, items);
    }

    /**
     * <b>Method to test:</b> {@link SitePaginator#getItems(User, String, int, int, String, OrderDirection, Map)} <br></br>
     * <b>Given Scenario:</b> SitePaginator is invoked with a dummy filter to get stopped and live hosts<br></br>
     * <b>Expected Result:</b> {@link HostAPI#search(String, boolean, boolean, boolean, int, int, User, boolean)} is expected to be called<br></br>
     */
    @Test
    public void test_GetItems_StoppedAndLiveSites(){
        final String filter = "filter";
        final boolean showArchived = false;
        final boolean showStopped = true;
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(hostAPI.search( filter, showArchived, showStopped,false, limit, offset, user, false ))
                .thenReturn( hosts );


        final PaginatedArrayList<Host> items = sitePaginator.getItems(user, filter, limit, offset, null, null,
                Map.of(SitePaginator.ARCHIVED_PARAMETER_NAME, showArchived));

        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(hosts, items);
    }


    /**
     * <b>Method to test:</b> {@link SitePaginator#getItems(User, String, int, int, String, OrderDirection, Map)} <br></br>
     * <b>Given Scenario:</b> SitePaginator is invoked with a dummy filter setting showSystemHost param<br></br>
     * <b>Expected Result:</b> {@link HostAPI#search(String, boolean, int, int, User, boolean)} is expected to be called<br></br>
     */
    @Test
    public void testGetItemsWithSystem(){
        final String filter = "filter";
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(hostAPI.search( filter,true, limit, offset, user, false ))
                .thenReturn( hosts );

        final PaginatedArrayList<Host> items = sitePaginator.getItems(user, filter, limit, offset, null, null,
                Map.of(SitePaginator.SYSTEM_PARAMETER_NAME, true));

        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(hosts, items);
    }
}
