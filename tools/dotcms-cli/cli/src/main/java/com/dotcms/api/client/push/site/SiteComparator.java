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

        // Compare by identifier
        return findByIdentifier(localSite.identifier(), serverContents);
    }

    @ActivateRequestContext
    @Override
    public Optional<SiteView> localContains(SiteView serverContent, List<SiteView> localSites) {

        // Compare by identifier
        return findByIdentifier(serverContent.identifier(), localSites);
    }

    @ActivateRequestContext
    @Override
    public boolean contentEquals(SiteView localSite, SiteView serverContent) {

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

}
