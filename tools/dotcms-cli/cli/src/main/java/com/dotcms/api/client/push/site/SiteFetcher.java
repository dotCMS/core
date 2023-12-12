package com.dotcms.api.client.push.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.ContentFetcher;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;

@Dependent
public class SiteFetcher implements ContentFetcher<SiteView> {

    @Inject
    protected RestClientFactory clientFactory;

    @ActivateRequestContext
    @Override
    public List<SiteView> fetch() {

        var siteAPI = clientFactory.getClient(SiteAPI.class);

        final int pageSize = 10;
        int page = 1;

        // Create a list to store all the retrieved sites
        List<SiteView> allSites = new ArrayList<>();

        while (true) {

            // Retrieve a page of sites
            ResponseEntityView<List<Site>> sitesResponse = siteAPI.getSites(
                    null,
                    null,
                    false,
                    false,
                    page,
                    pageSize
            );

            // Check if the response contains sites
            if (sitesResponse.entity() != null && !sitesResponse.entity().isEmpty()) {

                // Add the sites from the current page to the list
                List<SiteView> siteViews = sitesResponse.entity().stream()
                        .map(this::toView)
                        .collect(Collectors.toList());
                allSites.addAll(siteViews);

                // Increment the page number
                page++;
            } else {
                // Handle the case where the response doesn't contain sites or an error occurred
                break;
            }
        }

        return allSites;
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