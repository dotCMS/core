package com.dotcms.util.pagination;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/**
 * test {@link ContainerPaginator}
 */
public class ContainerPaginatorTest {

    @Test
    public void testGetContainers() throws DotDataException, DotSecurityException {
        int totalRecords = 10;
        User user = mock(User.class);
        String filter = "filter";
        Map<String, Object> params = map("title", filter);
        String hostId = "1";
        int offset = 5;
        int limit = 10;
        String orderby = "title";

        PaginatedArrayList<Container> containersExpected = new PaginatedArrayList<>();
        containersExpected.setTotalResults(totalRecords);

        ContainerFactory containerFactory = mock(ContainerFactory.class);

        when(containerFactory.findContainers(user, false, params, hostId,
                null, null, null, offset, limit, "title asc")).thenReturn(containersExpected);

        ContainerPaginator containerPaginator = new ContainerPaginator( containerFactory );

        PaginatedArrayList<Container> containers = containerPaginator.getItems(user, filter, limit, offset, orderby,
                OrderDirection.ASC, map(ContainerPaginator.HOST_PARAMETER_ID, hostId));

        assertEquals(containersExpected, containers);
        assertEquals(containers.getTotalResults(), totalRecords);
    }

    //@Test
    public void testGetContainersNullDirection() throws DotDataException, DotSecurityException {
        int totalRecords = 10;
        User user = mock(User.class);
        String filter = "filter";
        Map<String, Object> params = map("title", filter);
        String hostId = "1";
        int offset = 5;
        int limit = 10;
        String orderby = "title";

        PaginatedArrayList<Container> containersExpected = new PaginatedArrayList<>();
        containersExpected.setTotalResults(totalRecords);

        ContainerFactory containerFactory = mock(ContainerFactory.class);

        when(containerFactory.findContainers(user, false, params, hostId,
                null, null, null, offset, limit, "title")).thenReturn(containersExpected);

        ContainerPaginator containerPaginator = new ContainerPaginator( containerFactory );

        PaginatedArrayList<Container> containers = containerPaginator.getItems(user, filter, limit, offset, orderby, null, map());

        assertEquals(containersExpected, containers);
        assertEquals(containers.getTotalResults(), totalRecords);
    }
}
