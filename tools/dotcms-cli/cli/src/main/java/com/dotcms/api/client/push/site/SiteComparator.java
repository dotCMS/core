package com.dotcms.api.client.push.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.push.ContentComparator;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.SiteView;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

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
    public Optional<SiteView> findMatchingServerContent(SiteView localSite,
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
    public Optional<SiteView> localContains(SiteView serverContent, List<SiteView> localSites) {

        // Compare by identifier first.
        var result = findByIdentifier(serverContent.identifier(), localSites);

        if (result.isEmpty()) {

            // If not found by id, compare by name.
            result = findBySiteName(serverContent.siteName(), localSites);
        }

        return result;
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
