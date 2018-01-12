package com.dotcms.util.pagination;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.PaginatedArrayList;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

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

    public void testGetItemsWithArchived(){
        final String filter = "filter";
        final boolean showArchived = true;
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(hostAPI.search( filter, showArchived,false, limit, offset, user, false ))
                .thenReturn( hosts );

        final PaginatedArrayList<Host> items = sitePaginator.getItems(user, filter, limit, offset, null, null,
                map(SitePaginator.ARCHIVED_PARAMETER_NAME, showArchived));
        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(hosts, items);
    }

    public void testGetItemsWithStopped(){
        final String filter = "filter";
        final boolean showStopped = true;
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(hostAPI.searchByStopped( filter, !showStopped,false, limit, offset, user, false ))
                .thenReturn( hosts );

        final PaginatedArrayList<Host> items = sitePaginator.getItems(user, filter, limit, offset, null, null,
                map(SitePaginator.LIVE_PARAMETER_NAME, !showStopped));

        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(hosts, items);
    }

    public void testGetItemsWithStoppedAndArchived(){
        final String filter = "filter";
        final boolean showArchived = true;
        final boolean showStopped = true;
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(hostAPI.search( filter, showArchived, !showStopped,false, limit, offset, user, false ))
                .thenReturn( hosts );

        final PaginatedArrayList<Host> items = sitePaginator.getItems(user, filter, limit, offset, null, null,
                map(SitePaginator.ARCHIVED_PARAMETER_NAME, showArchived, SitePaginator.LIVE_PARAMETER_NAME, !showStopped));

        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(hosts, items);
    }

    public void testGetItemsWithSystem(){
        final String filter = "filter";
        final int limit = 5;
        final int offset = 4;
        final User user = new User();

        when(hostAPI.search( filter,true, limit, offset, user, false ))
                .thenReturn( hosts );

        final PaginatedArrayList<Host> items = sitePaginator.getItems(user, filter, limit, offset, null, null,
                map(SitePaginator.SYSTEM_PARAMETER_NAME, true));

        assertEquals(totalRecords, items.getTotalResults());
        assertEquals(hosts, items);
    }
}
