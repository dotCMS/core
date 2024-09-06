package com.dotcms.api.client.push.site;

import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.ContentFetcher;
import com.dotcms.api.client.util.SiteIterator;
import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

@Dependent
public class SiteFetcher implements ContentFetcher<SiteView> {

    @Inject
    protected RestClientFactory clientFactory;

    @ActivateRequestContext
    @Override
    public List<SiteView> fetch() {

        // Fetching the all the existing sites
        final List<Site> allSites = new ArrayList<>();

        final SiteIterator siteIterator = new SiteIterator(
                clientFactory,
                100
        );
        while (siteIterator.hasNext()) {
            List<Site> sites = siteIterator.next();
            allSites.addAll(sites);
        }

        // Looking for archived sites
        final var archivedSiteIterator = new SiteIterator(
                clientFactory,
                100,
                true,
                false,
                false
        );
        while (archivedSiteIterator.hasNext()) {
            List<Site> sites = archivedSiteIterator.next();
            allSites.addAll(sites);
        }

        // Add the sites from the current page to the list
        return allSites.stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    /**
     * Converts a Site object to a SiteView object.
     *
     * @param site the Site object to be converted
     * @return the converted SiteView object
     */
    private SiteView toView(final Site site) {

        return SiteView.builder()
                .identifier(site.identifier())
                .inode(site.inode())
                .aliases(site.aliases())
                .hostName(site.hostName())
                .systemHost(site.systemHost())
                .isDefault(site.isDefault())
                .isArchived(site.isArchived())
                .isLive(site.isLive())
                .isWorking(site.isWorking())
                .build();
    }

}