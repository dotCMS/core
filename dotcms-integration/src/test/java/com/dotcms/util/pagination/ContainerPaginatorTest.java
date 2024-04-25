package com.dotcms.util.pagination;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Jose Castro
 * @since Apr 26th, 2022
 */
public class ContainerPaginatorTest extends ContentletBaseTest {

    /**
     * Method to test: {@link ContainerPaginator#getItems(User, String, int, int, String, OrderDirection, Map)}
     *
     * Given Scenario: Get a paginated list of Containers based on specific filtering criteria. This method creates
     * three test Containers, but sets an offset value of 2.
     *
     * Expected Result: The list of paginated Containers with an offset of 2, which leaves 2 of the 3 test Containers
     * out of the result set. This will confirm that the pagination works.
     */
    @Test
    public void getPaginatedContainers() throws DotDataException, DotSecurityException {
        // Initialization
        final String ORDER_BY = "title";
        final String timestamp = String.valueOf(System.currentTimeMillis());
        final String filter = "Container Paginator Test " + timestamp;
        final int limit = 20;
        final int offset = 2;
        Container containerOne = null;
        Container containerTwo = null;
        Container containerThree = null;
        // Need to filter to this test.  There could be more than 20 containers in the system.
        final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
                .siteId(defaultHost.getIdentifier())
                .filteringCriteria("title", filter)
                .orderBy(ORDER_BY).build();
        List<Container> containerList = containerAPI.findContainers(user, searchParams);
        final int initialContainerCount = containerList.size();

        try {
            // Test data generation
            containerOne = new ContainerDataGen().site(defaultHost).title(filter+" 1").nextPersisted();
            containerTwo = new ContainerDataGen().site(defaultHost).title(filter+" 2").nextPersisted();
            containerThree = new ContainerDataGen().site(defaultHost).title(filter+" 3").nextPersisted();


            TestDataUtils.waitForEmptyQueue();

            final ContainerPaginator containerPaginator = new ContainerPaginator();
            final PaginatedArrayList<ContainerView> containersViews = containerPaginator.getItems(user, filter, limit, offset, ORDER_BY,
                    OrderDirection.ASC, Map.of(ContainerPaginator.HOST_PARAMETER_ID, defaultHost.getIdentifier()));
            containerList = containersViews.stream()
                    .map(containerView -> containerView.getContainer())
                    .collect(Collectors.toList());
            final int finalContainerCount = containerList.size();

            // Assertions
            assertEquals("There must be a difference of only 1 Container after comparing results!", 1,
                    finalContainerCount - initialContainerCount);
        } finally {
            // Data cleanup
            if (null != containerOne) {
                containerAPI.delete(containerOne, user, false);
            }
            if (null != containerTwo) {
                containerAPI.delete(containerTwo, user, false);
            }
            if (null != containerThree) {
                containerAPI.delete(containerThree, user, false);
            }
        }
    }

}
