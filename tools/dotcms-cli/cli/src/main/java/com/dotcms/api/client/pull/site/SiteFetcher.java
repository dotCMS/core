package com.dotcms.api.client.pull.site;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.pull.ContentFetcher;
import com.dotcms.api.client.util.SiteIterator;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.Site;
import com.dotcms.model.site.SiteView;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.context.ManagedExecutor;

@Dependent
public class SiteFetcher implements ContentFetcher<SiteView>, Serializable {

    private static final long serialVersionUID = 1082298802098576444L;

    @Inject
    protected RestClientFactory clientFactory;

    @Inject
    ManagedExecutor executor;

    @ActivateRequestContext
    @Override
    public List<SiteView> fetch(boolean failFast, Map<String, Object> customOptions) {

        // Fetching the all the existing sites
        final List<Site> allSites = allSites();

        // Create a HttpRequestTask to process the sites in parallel
        // We need this extra logic because the site API returns when calling all sites an object
        // that is not equal to the one returned when calling by id or by name, it is a reduced and
        // different version of a site, so we need to call the API for each site to get the full object.
        var task = new HttpRequestTask(this, executor);
        task.setTaskParams(allSites);

        return task.compute().join();
    }

    /**
     * Retrieves all sites.
     *
     * @return a list of Site objects containing all the existing sites, including archived sites
     */
    private List<Site> allSites() {

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

        return allSites;
    }

    @ActivateRequestContext
    @Override
    public SiteView fetchByKey(String siteNameOrId, boolean failFast,
            Map<String, Object> customOptions)
            throws NotFoundException {

        final var siteAPI = clientFactory.getClient(SiteAPI.class);

        if (siteNameOrId.replace("-", "").matches("[a-fA-F0-9]{32}")) {
            final ResponseEntityView<SiteView> byId = siteAPI.findById(siteNameOrId);
            return byId.entity();
        }

        final ResponseEntityView<SiteView> byId = siteAPI.findByName(
                GetSiteByNameRequest.builder().siteName(siteNameOrId).build());
        return byId.entity();
    }

}