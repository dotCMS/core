package com.dotcms.util.pagination;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.business.ContainerFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        final int totalRecords = 10;
        final User user = mock(User.class);
        final String filter = "filter";
        final Map<String, Object> params = map("title", filter);
        final String hostId = "1";
        final int offset = 5;
        final int limit = 10;
        final String orderby = "title";

        final PaginatedArrayList<Container> containersExpected = new PaginatedArrayList<>();
        containersExpected.setTotalResults(totalRecords);

        final ContainerAPI containerAPI = mock(ContainerAPI.class);

        when(containerAPI.findContainers(user, false, params, hostId,
                null, null, null, offset, limit, "title asc")).thenReturn(containersExpected);

        final ContainerPaginator containerPaginator = new ContainerPaginator( containerAPI );

        final PaginatedArrayList<ContainerView> containersViews = containerPaginator.getItems(user, filter, limit, offset, orderby,
                OrderDirection.ASC, map(ContainerPaginator.HOST_PARAMETER_ID, hostId));

        final List<Container> containers = containersViews.stream()
                .map(containerView -> containerView.getContainer())
                .collect(Collectors.toList());

        assertEquals(containersExpected, containers);
        assertEquals(containersViews.getTotalResults(), totalRecords);
    }

    //@Test
    public void testGetContainersNullDirection() throws DotDataException, DotSecurityException {
        final int totalRecords = 10;
        final User user = mock(User.class);
        final String filter = "filter";
        final Map<String, Object> params = map("title", filter);
        final String hostId = "1";
        final int offset = 5;
        final int limit = 10;
        final String orderby = "title";

        final PaginatedArrayList<Container> containersExpected = new PaginatedArrayList<>();
        containersExpected.setTotalResults(totalRecords);

        final ContainerAPI containerAPI = mock(ContainerAPI.class);

        when(containerAPI.findContainers(user, false, params, hostId,
                null, null, null, offset, limit, "title")).thenReturn(containersExpected);

        final ContainerPaginator containerPaginator = new ContainerPaginator( containerAPI );

        final PaginatedArrayList<ContainerView> containersViews = containerPaginator.getItems(user, filter, limit, offset, orderby, null, map());

        final List<Container> containers = containersViews.stream()
                .map(containerView -> containerView.getContainer())
                .collect(Collectors.toList());

        assertEquals(containersExpected, containers);
        assertEquals(containersViews.getTotalResults(), totalRecords);
    }
}
