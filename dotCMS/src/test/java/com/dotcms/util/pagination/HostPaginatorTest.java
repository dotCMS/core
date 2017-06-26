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

/**
 * test {@link HostPaginator}
 */
public class HostPaginatorTest {

    HostAPI hostAPI;
    HostPaginator hostPaginator;

    @Before
    public void init(){
        hostAPI = mock(HostAPI.class);
        hostPaginator = new HostPaginator( hostAPI );
    }

    @Test
    public void testGetItems(){
        String filter = "filter";
        boolean showArchived = true;
        int limit = 5;
        int offset = 4;
        User user = new User();
        int totalRecords = 5;

        PaginatedArrayList<Host> hosts = new PaginatedArrayList<>();
        hosts.setTotalResults( totalRecords );
        hosts.add( mock( Host.class ) );
        hosts.add( mock( Host.class ) );
        hosts.add( mock( Host.class ) );
        hosts.add( mock( Host.class ) );
        hosts.add( mock( Host.class ) );

        when(hostAPI.search( filter, showArchived, false, limit, offset, user, false ))
                .thenReturn( hosts );

        Collection<Host> items = hostPaginator.getItems(user, filter, showArchived, limit, offset, null, null);

        assertEquals(totalRecords, hostPaginator.getTotalRecords(filter));
        assertEquals(hosts, items);
    }


}
