package com.dotcms.rest.api.v1.apps;

import com.dotcms.rest.api.v1.apps.view.SiteView;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginationException;
import com.dotcms.util.pagination.PaginatorOrdered;
import com.dotmarketing.beans.Host;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * PaginatorOrdered implementation for objects of type SiteView.
 * This Pagination isn't very typical in the sense that items are not retrieved from a database.
 * Sorting and filtering itself happens right here on top of the list of elements itself.
 */
public class SiteViewPaginator implements PaginatorOrdered<SiteView> {

    private static final String CONTENT_TYPE_HOST_QUERY = "+contentType:Host +working:true ";
    private static final String CONTENT_TYPE_HOST_WITH_TITLE_QUERY = "+contentType:Host +working:true +title:*%s*";

    private final Supplier<Set<String>> configuredSitesSupplier;
    private final HostAPI hostAPI;
    private final ContentletAPI contentletAPI;

    @VisibleForTesting
    public SiteViewPaginator(final Supplier<Set<String>> configuredSitesSupplier,
            final HostAPI hostAPI, final ContentletAPI contentletAPI) {
        this.configuredSitesSupplier = configuredSitesSupplier;
        this.hostAPI = hostAPI;
        this.contentletAPI = contentletAPI;
    }

    /**
     * This custom getItem implementation will extract join and apply filtering sorting and pagination.
     * @param user user to filter
     * @param filter extra filter parameter
     * @param limit Number of items to return
     * @param offset offset
     * @param orderBy This param is ignored.
     * @param direction This param is ignored.
     * @param extraParams This param is ignored.
     * @return SiteView pageItems.
     * @throws PaginationException
     */
    @Override
    public PaginatedArrayList<SiteView> getItems(final User user, final String filter,
            final int limit, final int offset,
            final String orderBy, final OrderDirection direction,
            final Map<String, Object> extraParams) throws PaginationException {
        try {
            //get all sites. Even though this comes from the index. it is permissions driven.
            final List<String> allSitesIdentifiers = getHostIdentifiers(user, filter);

            final long totalCount = allSitesIdentifiers.size();

            //This values are fed from the outside through the serviceIntegrationAPI.
            final Set<String> sitesWithConfigurations = configuredSitesSupplier.get();
            final LinkedHashSet<String> allSites = new LinkedHashSet<>(allSitesIdentifiers);

            //By doing this we remove from the configured-sites collection whatever sites didn't match the search.
            //If it isn't part of the search results also discard from the configured sites we intent to show.
            final LinkedHashSet<String> configuredSites = sitesWithConfigurations.stream()
                    .filter(allSites::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            final List<String> finalList = join(configuredSites, allSitesIdentifiers).stream()
                    .skip(offset).limit(limit).collect(Collectors.toList());

            //And finally load from db and map into the desired view.
            final List<SiteView> siteViews = finalList.stream().map(id -> {
                try {
                    return hostAPI.find(id, user, false);
                } catch (DotDataException | DotSecurityException e) {
                    Logger.error(SiteViewPaginator.class, e);
                }
                return null;
            }).filter(Objects::nonNull).map(host -> {
                final boolean configured = configuredSites.contains(host.getIdentifier().toLowerCase());
                return new SiteView(host.getIdentifier(), host.getName(), configured);
            }).collect(Collectors.toList());

            //And then we're done and out of here.
            final PaginatedArrayList<SiteView> paginatedArrayList = new PaginatedArrayList<>();
            paginatedArrayList.setTotalResults(totalCount);
            paginatedArrayList.addAll(siteViews);
            return paginatedArrayList;

        } catch (Exception e) {
            Logger.error(SiteViewPaginator.class, e.getMessage(), e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * This join method expects two collections of host identifiers.
     * First-one the Set coming from the serviceIntegrations-API containing all the sites with configurations.
     * Second-one a List with all the sites coming from querying the index.
     * Meaning this list is expected to come filtered and sorted.
     * The resulting list will have all the configured items first and then he rest of the entries.
     * Additionally to that SYSTEM_HOST is always expected to appear first if it ever existed on the allSites list.
     * (Cuz it could have been removed from applying filtering).
     * @param configuredSites sites with configurations coming from service Integration API.
     * @param allSites all-sites sorted and filtered loaded from ES
     * @return a brand new List ordered.
     */
    private List<String> join(final Set<String> configuredSites, final List<String> allSites) {
        final List<String> newList = new LinkedList<>();
        boolean systemHostFound = false;
        int index = 0;
        for (final String siteIdentifier : allSites) {
            if (!siteIdentifier.equalsIgnoreCase(Host.SYSTEM_HOST)) {
                if (configuredSites.contains(siteIdentifier)) {
                    newList.add(index++, siteIdentifier);
                } else {
                    newList.add(siteIdentifier);
                }
            } else {
                systemHostFound = true;
            }
        }
        if (systemHostFound) {
            newList.add(0, Host.SYSTEM_HOST);
        }
        return newList;
    }

    /**
     * Load all host identifiers from index
     * internally this includes permissions into the query.
     * So it is very performant.
     * The results are returned by default in order Ascendant order by site name (internally content-title).
     * This is very important cause any comparator applied must respect that.
     * The identifier SYSTEM_HOST is returned in lower case by the index. If that ever changes this will be broken.
     * @param user logged-in user
     * @param filter a string to match against the title.
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private List<String> getHostIdentifiers(final User user, final String filter)
            throws DotDataException, DotSecurityException {
        //get all sites. This is permissions driven.
        final String query = UtilMethods.isSet(filter) ? String
                .format(CONTENT_TYPE_HOST_WITH_TITLE_QUERY, filter) : CONTENT_TYPE_HOST_QUERY;
        //This returns a list with all the hosts
        final List<ContentletSearch> allSitesIdentifiers = contentletAPI
                .searchIndex(query, 0, 0, "title", user, false);
        return allSitesIdentifiers.stream().filter(Objects::nonNull)
                .map(ContentletSearch::getIdentifier)
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

}
