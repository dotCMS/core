package com.dotcms.util.pagination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link ContainerPaginator}.
 */
class ContainerPaginatorUnitTest {

    /**
     * Method to test: {@link ContainerPaginator#getItems}
     * Given: the {@link ContainerPaginator#CONTENT_TYPE} extra parameter holds a content type id
     * When: the paginator asks the container API for a page
     * Then: the search parameters handed to {@code ContainerAPI#findContainers(User, SearchParams)}
     *       carry that content type id, and the call is made for the requesting user
     */
    @Test
    void getItems_withContentType_searchesByThatContentType() throws Exception {
        final String contentTypeId = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4";
        final User user = mock(User.class);
        final ContainerAPI containerAPI = mock(ContainerAPI.class);
        when(containerAPI.findContainers(any(User.class), any(ContainerAPI.SearchParams.class)))
                .thenReturn(new PaginatedArrayList<Container>());

        new ContainerPaginator(containerAPI, mock(HostWebAPI.class)).getItems(user, "", 10, 0,
                "title", OrderDirection.ASC, Map.of(ContainerPaginator.CONTENT_TYPE, contentTypeId));

        final ArgumentCaptor<ContainerAPI.SearchParams> searchParams =
                ArgumentCaptor.forClass(ContainerAPI.SearchParams.class);
        final ArgumentCaptor<User> requestingUser = ArgumentCaptor.forClass(User.class);
        verify(containerAPI).findContainers(requestingUser.capture(), searchParams.capture());
        assertSame(user, requestingUser.getValue());
        assertEquals(contentTypeId, searchParams.getValue().contentTypeIdOrVar());
    }

}
