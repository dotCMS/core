package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Source;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Handle {@link com.dotmarketing.portlets.containers.model.Container} pagination
 */
public class ContainerPaginator implements PaginatorOrdered<Container> {

    public static final String HOST_PARAMETER_ID = "host";

    private final ContainerAPI containerAPI;

    public ContainerPaginator() {
        containerAPI = APILocator.getContainerAPI();
    }

    @VisibleForTesting
    public ContainerPaginator(final ContainerAPI containerAPI) {
        this.containerAPI = containerAPI;
    }

    @Override
    public PaginatedArrayList<Container> getItems(final User user, final String filter, final int limit, final int offset,
                                                  final String orderby, final OrderDirection direction,
                                                  final Map<String, Object> extraParams) {
        String hostId = null;

        if (extraParams != null) {
            hostId = (String) extraParams.get(HOST_PARAMETER_ID);
        }

        final Map<String, Object> params = map("title", filter);

        String orderByDirection = orderby;
        if (UtilMethods.isSet(direction) && UtilMethods.isSet(orderby)) {
            orderByDirection = new StringBuffer(orderByDirection)
                    .append(" ")
                    .append(direction.toString().toLowerCase()).toString();
        }

        try {
            final PaginatedArrayList<Container> allContainers =
                    (PaginatedArrayList<Container>) containerAPI.findContainers(user, false, params, hostId,
                    null, null, null, offset, limit, orderByDirection);

            if (!UtilMethods.isSet(hostId)) {

                final List<Container> fileContainers = new ArrayList<>();
                final List<Container> dbContainers   = new ArrayList<>();

                for (final Container container : allContainers) {

                    if (container.getSource() == Source.DB) {

                        dbContainers.add  (container);
                    } else {
                        fileContainers.add(container);
                    }
                }

                if (direction == OrderDirection.ASC) {

                    dbContainers.stream().sorted   (Comparator.comparing(this::hostname));
                    fileContainers.stream().sorted (Comparator.comparing(this::hostname));
                } else {

                    dbContainers.stream().sorted   (Comparator.comparing(this::hostname).reversed());
                    fileContainers.stream().sorted (Comparator.comparing(this::hostname).reversed());
                }


                final PaginatedArrayList<Container> sortedByHostContainers = new PaginatedArrayList<>();
                sortedByHostContainers.setQuery(allContainers.getQuery());
                sortedByHostContainers.setTotalResults(allContainers.getTotalResults());

                sortedByHostContainers.addAll(fileContainers);
                sortedByHostContainers.addAll(dbContainers);
                return sortedByHostContainers;
            }

            return allContainers;
        } catch (DotSecurityException|DotDataException e) {
            throw new PaginationException(e);
        }
    }

    private String hostname (final Container container) {

        try {
            return Host.class.cast(container.getParentPermissionable()).getHostname();
        } catch (DotDataException e) {
            return StringPool.BLANK;
        }
    }

    private int compareByHost (final Container container1, final Container container2) {

        try {
            return Host.class.cast(container1.getParentPermissionable()).getHostname().compareTo(Host.class.cast(container2.getParentPermissionable()).getHostname());
        } catch (DotDataException e) {
            return 0;
        }
    }
}
