package com.dotcms.common;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.site.GetSiteByNameRequest;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

public class SiteTestHelper {

    @Inject
    RestClientFactory clientFactory;

    /**
     * Checks if a site with the given name exists.
     *
     * @param siteName the name of the site to check
     * @return true if the site exists, false otherwise
     */
    protected Boolean siteExist(final String siteName) {
        final var siteAPI = clientFactory.getClient(SiteAPI.class);
        return siteExist(siteName, siteAPI);
    }

    /**
     * Checks if a site with the given name exists.
     *
     * @param siteName the name of the site to check
     * @param siteAPI  the site api to use
     * @return true if the site exists, false otherwise
     */
    protected Boolean siteExist(final String siteName, final SiteAPI siteAPI) {

        long start = System.currentTimeMillis();
        long end = start + 15 * 1000; // 15 seconds * 1000 ms/sec
        while (System.currentTimeMillis() < end) {
            try {
                var response = siteAPI.findByName(
                        GetSiteByNameRequest.builder().siteName(siteName).build()
                );
                if ((response != null && response.entity() != null) &&
                        ((response.entity().isLive() != null &&
                                response.entity().isLive()) &&
                                (response.entity().isWorking() != null &&
                                        response.entity().isWorking()))) {
                    return true;
                }
            } catch (NotFoundException e) {
                // Do nothing
            }

            try {
                Thread.sleep(2000); // Sleep for 2 second
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        return false;
    }

    /**
     * Checks if the site statuses are valid.
     *
     * @param siteName The name of the site to check.
     * @param isLive   The expected live status of the site.
     * @param archive  The expected archive status of the site.
     * @return True if the site statuses are valid, false otherwise.
     */
    protected Boolean checkValidSiteStatus(final String siteName,
            final boolean isLive, final boolean archive) {

        long start = System.currentTimeMillis();
        long end = start + 15 * 1000; // 15 seconds * 1000 ms/sec
        while (System.currentTimeMillis() < end) {
            try {
                var response = clientFactory.getClient(SiteAPI.class)
                        .findByName(GetSiteByNameRequest.builder().siteName(siteName).build());
                if ((response != null && response.entity() != null) &&
                        ((response.entity().isLive() != null &&
                                response.entity().isLive().equals(isLive)) &&
                                (response.entity().isArchived() != null &&
                                        response.entity().isArchived().equals(archive)))) {
                    return true;
                }
            } catch (NotFoundException e) {
                // Do nothing
            }

            try {
                Thread.sleep(2000); // Sleep for 2 second
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        return false;
    }
}
