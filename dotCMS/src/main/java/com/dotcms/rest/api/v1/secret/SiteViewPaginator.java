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
 * This Pagination isn't very typical in the sense that items are not retrieved from a database.
 * Sorting and filtering itself happens right here on top of the list of elements itself.
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

    /**
     * if consumer of this class decides to pass ServiceDescriptor in the extraParams
     * This will  return that. Otherwise it will still attempt to retrieve the ServiceDescriptor by looking up the serviceKey.
     * This is just an extra validation to ensure we're gonna try to retrive items from an existing service.
     * @param user logged in user.
     * @param extraParams params passed o getItems Method
     * @return Optional of ServiceDescriptor
     * @throws DotSecurityException
     * @throws DotDataException
     */
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

    /**
     * This custom getItem implementation will extract join and apply filtering sorting and pagination.
     * @param user user to filter
     * @param filter extra filter parameter
     * @param limit Number of items to return
     * @param offset offset
     * @param orderBy
     * @param direction If the order is Asc or Desc
     * @param extraParams
     * @return
     * @throws PaginationException
     */
    @Override
    public PaginatedArrayList<SiteView> getItems(final User user, final String filter,
            final int limit, final int offset,
            final String orderBy, final OrderDirection direction,
            final Map<String, Object> extraParams) throws PaginationException {
        try {
            final Optional<ServiceDescriptor> serviceDescriptorOptional = getServiceDescriptor(user,
                    extraParams);
            if (serviceDescriptorOptional.isPresent()) {

                //Just making sure there's a descriptor to get items.
                final ServiceDescriptor serviceDescriptor = serviceDescriptorOptional.get();

                //All the sites set.
                final List<Host> allSites = hostAPI.findAll(user, false);

                //Sites with configuration.
                final List<Host> sitesWithIntegrations = serviceIntegrationAPI
                        .getSitesWithIntegrations(user);

                //Sites with configuration - for the respective serviceKey passed.
                final List<Host> sitesForService = serviceIntegrationAPI
                        .filterSitesForService(serviceDescriptor.getKey(), sitesWithIntegrations,
                                user);

                //Complement operation against to get sites without any configuration.
                final Set<Host> nonConfiguredSites = Sets.difference(
                        Sets.newHashSet(allSites), Sets.newHashSet(sitesForService)
                );

                //Make them all SiteView and mark'em respective.
                final Stream<SiteView> withIntegrationsStream = sitesForService.stream()
                        .map(site -> new SiteView(site.getIdentifier(), site.getHostname(), true));
                final Stream<SiteView> withNoIntegrations = nonConfiguredSites.stream()
                        .map(site -> new SiteView(site.getIdentifier(), site.getHostname(), false));

                Stream<SiteView> combinedStream = Stream
                        .concat(withIntegrationsStream, withNoIntegrations);

                final long combinedCount = combinedStream.count();

                //if we have filter apply it.
                if (UtilMethods.isSet(filter)) {
                    combinedStream = combinedStream
                            .filter(siteView -> siteView.getName()
                                    .matches("(.*)" + filter + "(.*)"));
                }

                //apply pagination params.
                combinedStream = combinedStream.skip(offset).limit(limit);

                //Setup sorting
                final Comparator<SiteView> comparator = orderByAndDirection(orderBy, direction);

                final List<SiteView> siteViews = combinedStream.sorted(comparator)
                        .collect(Collectors.toList());

                //And then we're done and out of here.
                final PaginatedArrayList<SiteView> paginatedArrayList = new PaginatedArrayList<>();
                paginatedArrayList.setTotalResults(combinedCount);
                paginatedArrayList.addAll(siteViews);
                return paginatedArrayList;
            }
            return new PaginatedArrayList<>();
        } catch (Exception e) {
            Logger.error(SiteViewPaginator.class, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Given two pagination params (orderBy and direction)
     * This will get you the proper SiteView comparator.
     * @param orderBy
     * @param direction
     * @return
     */
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

    private static Comparator<SiteView> hasIntegrationsDescComparator = (a, b) -> {
        final int i = Boolean.compare(b.isIntegrations(), a.isIntegrations());
        if (i != 0) {
            return i;
        }
        return a.getName().compareTo(b.getName());
    };

    private static Comparator<SiteView> hasIntegrationsAscComparator = (a, b) -> {
        final int i = Boolean.compare(a.isIntegrations(), b.isIntegrations());
        if (i != 0) {
            return i;
        }
        return a.getName().compareTo(b.getName());
    };

    private static Comparator<SiteView> nameAscComparator = (a, b) -> a.getName().compareTo(b.getName());

    private static Comparator<SiteView> nameDescComparator = (a, b) -> b.getName().compareTo(a.getName());

    private static Comparator<SiteView> idAscComparator = (a, b) -> a.getId().compareTo(b.getId());

    private static Comparator<SiteView> idDescComparator = (a, b) -> b.getId().compareTo(a.getId());

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
