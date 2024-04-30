package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Source;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerView;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handle {@link com.dotmarketing.portlets.containers.model.Container} pagination
 */
public class ContainerPaginator implements PaginatorOrdered<ContainerView> {

    public static final String HOST_PARAMETER_ID = "host";
    public static final String SYSTEM_PARAMETER_NAME = "system";
    public static final String ARCHIVE_PARAMETER_NAME = "archive";
    public static final String CONTENT_TYPE = "content_type";

    private final ContainerAPI containerAPI;
    private final HostWebAPI hostWebAPI;

    public ContainerPaginator() {
        containerAPI = APILocator.getContainerAPI();
        hostWebAPI = WebAPILocator.getHostWebAPI();
    }

    @VisibleForTesting
    public ContainerPaginator(final ContainerAPI containerAPI, final HostWebAPI hostWebAPI) {
        this.containerAPI = containerAPI;
        this.hostWebAPI = hostWebAPI;
    }

    @Override
    public PaginatedArrayList<ContainerView> getItems(final User user, final String filter, final int limit, final int offset,
                                                  final String orderby, final OrderDirection direction,
                                                  final Map<String, Object> extraParams) {
        String siteId = null;
        boolean showSystemContainer = Boolean.FALSE;
        boolean showArchiveContainer = Boolean.FALSE;
        String contentTypeIdOrVar = StringPool.BLANK;

        if (extraParams != null) {
            siteId = (String) extraParams.get(HOST_PARAMETER_ID);
            showSystemContainer = Boolean.valueOf(String.valueOf(extraParams.get(SYSTEM_PARAMETER_NAME)));
            showArchiveContainer = Boolean.valueOf(String.valueOf(extraParams.get(ARCHIVE_PARAMETER_NAME)));
            contentTypeIdOrVar = String.valueOf(extraParams.get(CONTENT_TYPE));
        }

        final Map<String, Object> params = Map.of("title", filter);

        String orderByDirection = orderby;
        if (UtilMethods.isSet(direction) && UtilMethods.isSet(orderby)) {
            orderByDirection = new StringBuffer(orderByDirection)
                    .append(" ")
                    .append(direction.toString().toLowerCase()).toString();
        }

        try {
            final ContainerAPI.SearchParams searchParams = ContainerAPI.SearchParams.newBuilder()
                    .includeArchived(showArchiveContainer)
                    .includeSystemContainer(showSystemContainer)
                    .contentTypeIdOrVar(contentTypeIdOrVar)
                    .filteringCriterion(params)
                    .siteId(siteId)
                    // Include the System Container in the first result page only
                    .offset(showSystemContainer && offset > 0 ? offset - 1 : offset)
                    .limit(limit)
                    .orderBy(orderByDirection).build();
            final PaginatedArrayList<Container> allContainers =
                    (PaginatedArrayList<Container>) this.containerAPI.findContainers(user, searchParams);

            final PaginatedArrayList<Container> containers = !UtilMethods.isSet(siteId)
                    ? sortByTypeAndHost(direction, allContainers) : allContainers;

            return createContainerView(containers);
        } catch (final DotSecurityException | DotDataException e) {
            throw new PaginationException(e);
        }
    }

    private PaginatedArrayList<ContainerView> createContainerView(
            final PaginatedArrayList<Container> allContainers) throws DotDataException, DotSecurityException {
        final Host currentHost = hostWebAPI.getCurrentHost();

        final PaginatedArrayList containerViews = new PaginatedArrayList();
        containerViews.setTotalResults(allContainers.getTotalResults());
        containerViews.setQuery(allContainers.getQuery());
        containerViews.addAll(
                allContainers.stream()
                        .map(container -> new ContainerView(container))
                        .collect(Collectors.toList())
        );
        return containerViews;
    }

    /**
     * Sort {@link Container} by type (first all the {@link com.dotmarketing.portlets.containers.model.FileAssetContainer})
     * and host.
     *
     * @param direction
     * @param allContainers
     * @return
     */
    private PaginatedArrayList<Container> sortByTypeAndHost(
            final OrderDirection direction,
            final PaginatedArrayList<Container> allContainers) {

        final List<Container> fileContainers = new ArrayList<>();
        final List<Container> dbContainers   = new ArrayList<>();
        final List<Container> systemContainers   = new ArrayList<>();

        for (final Container container : allContainers) {

            if (container.getSource() == Source.DB) {
                if(container.getInode().equals(Container.SYSTEM_CONTAINER) && container.getIdentifier().equals(Container.SYSTEM_CONTAINER)){
                    systemContainers.add(container);
                }
                else {
                    dbContainers.add(container);
                }
            } else {
                fileContainers.add(container);
            }
        }

        if (direction == OrderDirection.ASC) {
            systemContainers.stream().sorted(Comparator.comparing(this::hostname));
            dbContainers.stream().sorted(Comparator.comparing(this::hostname));
            fileContainers.stream().sorted(Comparator.comparing(this::hostname));
        } else {
            systemContainers.stream().sorted(Comparator.comparing(this::hostname).reversed());
            dbContainers.stream().sorted(Comparator.comparing(this::hostname).reversed());
            fileContainers.stream().sorted(Comparator.comparing(this::hostname).reversed());
        }

        final PaginatedArrayList<Container> sortedByHostContainers = new PaginatedArrayList<>();
        sortedByHostContainers.setQuery(allContainers.getQuery());
        sortedByHostContainers.setTotalResults(allContainers.getTotalResults());

        sortedByHostContainers.addAll(systemContainers);
        sortedByHostContainers.addAll(dbContainers);
        sortedByHostContainers.addAll(fileContainers);
        return sortedByHostContainers;
    }

    private String hostname (final Container container) {

        try {
            return Host.class.cast(container.getParentPermissionable()).getHostname();
        } catch (DotDataException e) {
            return StringPool.BLANK;
        }
    }

}
