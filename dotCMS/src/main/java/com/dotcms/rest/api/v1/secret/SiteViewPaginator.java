package com.dotcms.rest.api.v1.secret;

import com.dotcms.rest.api.v1.secret.view.SiteView;
import com.dotcms.security.secret.ServiceDescriptor;
import com.dotcms.security.secret.ServiceIntegrationAPI;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginationException;
import com.dotcms.util.pagination.PaginatorOrdered;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.liferay.portal.model.User;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PaginatorOrdered implementation for objects of type SiteView.
 */
public class SiteViewPaginator implements PaginatorOrdered<SiteView> {

    static final String SERVICE_DESCRIPTOR = "SERVICE_DESCRIPTOR";
    static final String SERVICE_KEY = "SERVICE_KEY";

    private final ServiceIntegrationAPI serviceIntegrationAPI;
    private final HostAPI hostAPI;

    @VisibleForTesting
    SiteViewPaginator(final ServiceIntegrationAPI serviceIntegrationAPI,
            final HostAPI hostAPI) {
        this.serviceIntegrationAPI = serviceIntegrationAPI;
        this.hostAPI = hostAPI;
    }

    private Optional<ServiceDescriptor> getServiceDescriptor(final User user,
            final Map<String, Object> extraParams)
            throws DotSecurityException, DotDataException {
        ServiceDescriptor serviceDescriptor = (ServiceDescriptor) extraParams
                .get(SERVICE_DESCRIPTOR);
        if (null == serviceDescriptor) {
            final String serviceKey = (String) extraParams.get(SERVICE_KEY);
            if (UtilMethods.isSet(serviceKey)) {
                return serviceIntegrationAPI.getServiceDescriptor(serviceKey, user);
            }
            return Optional.empty();
        }
        return Optional.of(serviceDescriptor);
    }

    @Override
    public PaginatedArrayList<SiteView> getItems(final User user, final String filter,
            final int limit, final int offset,
            final String orderBy, final OrderDirection direction,
            final Map<String, Object> extraParams) throws PaginationException {
        try {
            final Optional<ServiceDescriptor> serviceDescriptorOptional = getServiceDescriptor(user,
                    extraParams);
            if (serviceDescriptorOptional.isPresent()) {

                final ServiceDescriptor serviceDescriptor = serviceDescriptorOptional.get();
                final List<Host> allSites = hostAPI.findAll(user, false);

                final List<Host> sitesWithIntegrations = serviceIntegrationAPI
                        .getSitesWithIntegrations(user);

                final List<Host> sitesForService = serviceIntegrationAPI
                        .filterSitesForService(serviceDescriptor.getKey(), sitesWithIntegrations,
                                user);

                final Set<Host> nonConfiguredSites = Sets.difference(
                        Sets.newHashSet(allSites), Sets.newHashSet(sitesForService)
                );

                final Stream<SiteView> withIntegrationsStream = sitesForService.stream()
                        .map(site -> new SiteView(site.getIdentifier(), site.getHostname(), true));
                final Stream<SiteView> withNoIntegrations = nonConfiguredSites.stream()
                        .map(site -> new SiteView(site.getIdentifier(), site.getHostname(), false));

                Stream<SiteView> combinedStream = Stream
                        .concat(withIntegrationsStream, withNoIntegrations);
                if (UtilMethods.isSet(filter)) {
                    combinedStream = combinedStream
                            .filter(siteView -> siteView.getName()
                                    .matches("(.*)" + filter + "(.*)"));
                }

                combinedStream = combinedStream.skip(offset).limit(limit);

                final Comparator<SiteView> comparator = orderByAndDirection(orderBy, direction);

                final List<SiteView> siteViews = combinedStream.sorted(comparator)
                        .collect(Collectors.toList());

                final PaginatedArrayList<SiteView> paginatedArrayList = new PaginatedArrayList<>();
                paginatedArrayList.setTotalResults(siteViews.size());
                paginatedArrayList.addAll(siteViews);
                return paginatedArrayList;
            }
            return new PaginatedArrayList<>();
        } catch (Exception e) {
            Logger.error(SiteViewPaginator.class, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    private static Comparator<SiteView> orderByAndDirection(final String orderBy,
            final OrderDirection direction) {
        final Map<OrderDirection, Comparator<SiteView>> directionComparatorMap = fieldAndDirectionComparatorsMap
                .get(orderBy.toLowerCase());
        if (null != directionComparatorMap) {
            Comparator<SiteView> comparator = directionComparatorMap.get(direction);
            if (null != comparator) {
                return comparator;
            }
        }
        //Default comparator
        return hasIntegrationsDescComparator;
    }

    private static Comparator<SiteView> hasIntegrationsDescComparator = (o1, o2) -> {
        final int i = Boolean.compare(o2.isIntegrations(), o1.isIntegrations());
        if (i != 0) {
            return i;
        }
        return o1.getName().compareTo(o2.getName());
    };

    private static Comparator<SiteView> hasIntegrationsAscComparator = (o1, o2) -> {
        final int i = Boolean.compare(o1.isIntegrations(), o2.isIntegrations());
        if (i != 0) {
            return i;
        }
        return o1.getName().compareTo(o2.getName());
    };

    private static Comparator<SiteView> nameAscComparator = (o1, o2) -> o1.getName().compareTo(o2.getName());

    private static Comparator<SiteView> nameDescComparator = (o1, o2) -> o2.getName().compareTo(o1.getName());

    private static Comparator<SiteView> idAscComparator = (o1, o2) -> o1.getId().compareTo(o2.getId());

    private static Comparator<SiteView> idDescComparator = (o1, o2) -> o2.getId().compareTo(o1.getId());

    private static Map<String, Map<OrderDirection, Comparator<SiteView>>> fieldAndDirectionComparatorsMap = ImmutableMap
            .of(
                    "integrations", ImmutableMap
                            .of(OrderDirection.ASC, hasIntegrationsAscComparator,
                                OrderDirection.DESC, hasIntegrationsDescComparator),
                    "name", ImmutableMap
                            .of(OrderDirection.ASC, nameAscComparator,
                                OrderDirection.DESC, nameDescComparator),
                    "id", ImmutableMap
                            .of(OrderDirection.ASC, idAscComparator,
                                OrderDirection.DESC, idDescComparator)
            );

}
