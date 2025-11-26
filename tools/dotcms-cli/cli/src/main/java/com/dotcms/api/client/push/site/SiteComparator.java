package com.dotcms.api.client.push.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.ContentComparator;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

@Dependent
public class SiteComparator implements ContentComparator<SiteView> {

    @Inject
    protected RestClientFactory clientFactory;

    @Override
    public Class<SiteView> type() {
        return SiteView.class;
    }

    @ActivateRequestContext
    @Override
    public Optional<SiteView> findMatchingServerContent(File localFile, SiteView localSite,
            List<SiteView> serverContents) {

        // Compare by identifier first.
        var result = findByIdentifier(localSite.identifier(), serverContents);

        if (result.isEmpty()) {

            // If not found by id, compare by name.
            result = findBySiteName(localSite.siteName(), serverContents);
        }

        return result;
    }

    @ActivateRequestContext
    @Override
    public boolean existMatchingLocalContent(SiteView serverContent, List<File> localFiles,
            List<SiteView> localSites) {

        // Compare by identifier first.
        var result = findByIdentifier(serverContent.identifier(), localSites);

        if (result.isEmpty()) {

            // If not found by id, compare by name.
            result = findBySiteName(serverContent.siteName(), localSites);
        }

        return result.isPresent();
    }

    @ActivateRequestContext
    @Override
    public boolean contentEquals(SiteView localSite, SiteView serverContent) {

        // Validation to make sure the equals method works as expected
        localSite = setDefaultsForNoValue(localSite);

        // Looking for the site in the server by identifier, this call is necessary because the
        // serverContent comes from the `getSites` call which doesn't return all the fields.
        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);
        final ResponseEntityView<SiteView> byId = siteAPI.findById(serverContent.identifier());

        // Comparing the local and server content in order to determine if we need to update or
        // not the content
        return localSite.equals(byId.entity());
    }

    /**
     * Returns a comparator for sorting SiteView objects based on their processing order.
     * <p>
     * The processing order is determined by whether the SiteView objects have the "default" flag
     * set. SiteView objects with the "default" flag set to true will be placed before SiteView
     * objects with the "default" flag set to false.
     * <p>
     * This processing order based on the "default" flag is necessary because changing the default
     * site affects not only the site itself, but also the previous default site. Processing sites
     * in the proper order allows us to update properly all the affected sites descriptors when the
     * default site is changed.
     *
     * @return a comparator for sorting SiteView objects based on their processing order
     */
    @Override
    public Comparator<SiteView> getProcessingOrderComparator() {
        return (site1, site2) ->
                Boolean.compare(
                        Boolean.TRUE.equals(site2.isDefault()),
                        Boolean.TRUE.equals(site1.isDefault())
                );
    }

    /**
     * Finds a SiteView object in the given list based on the specified identifier.
     *
     * @param identifier the identifier of the SiteView object to be found
     * @param sites      the list of Sites objects to search in
     * @return an Optional containing the found SiteView object, or an empty Optional if no match is
     * found
     */
    private Optional<SiteView> findByIdentifier(String identifier, List<SiteView> sites) {

        if (identifier != null && !identifier.isEmpty()) {
            for (var site : sites) {
                if (site.identifier() != null && site.identifier().equals(identifier)) {
                    return Optional.of(site);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Finds a SiteView object in the given list based on the specified site name.
     *
     * @param siteName the site name of the SiteView object to be found
     * @param sites    the list of SiteView objects to search in
     * @return an Optional containing the found SiteView object, or an empty Optional if no match is
     * found
     */
    private Optional<SiteView> findBySiteName(String siteName, List<SiteView> sites) {

        if (siteName != null && !siteName.isEmpty()) {
            for (var site : sites) {
                if (site.siteName() != null && site.siteName().equalsIgnoreCase(siteName)) {
                    return Optional.of(site);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Sets default empty values for a SiteView object.
     *
     * @param site the SiteView object for which the default empty values need to be set
     * @return the SiteView object with default empty values set
     */
    private SiteView setDefaultsForNoValue(SiteView site) {

        // Validation to make sure the equals method works as expected
        if (site.systemHost() == null) {
            site = site.withSystemHost(false);
        }
        if (site.siteThumbnail() == null) {
            site = site.withSiteThumbnail("");
        }
        if (site.runDashboard() == null) {
            site = site.withRunDashboard(false);
        }
        if (site.isDefault() == null) {
            site = site.withIsDefault(false);
        }
        if (site.isArchived() == null) {
            site = site.withIsArchived(false);
        }
        if (site.isLive() == null) {
            site = site.withIsLive(false);
        }
        if (site.isWorking() == null) {
            site = site.withIsWorking(false);
        }
        if (site.isLocked() == null) {
            site = site.withIsLocked(false);
        }

        return site;
    }

}
