package com.dotcms.common;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.SiteView;
import java.time.Duration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

@ApplicationScoped
public class SiteTestHelperService {

    private static final Duration MAX_WAIT_TIME = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    @Inject
    RestClientFactory clientFactory;

    /**
     * Checks if the site statuses are valid.
     *
     * @param siteName The name of the site to check.
     * @param isLive   The expected live status of the site.
     * @param archive  The expected archive status of the site.
     * @return True if the site statuses are valid, false otherwise.
     */
    public Boolean checkValidSiteStatus(final String siteName,
            final boolean isLive, final boolean archive) {

        try {

            await()
                    .atMost(MAX_WAIT_TIME)
                    .pollInterval(POLL_INTERVAL)
                    .until(() -> {
                        try {
                            var response = findSiteByName(siteName);
                            return (response != null && response.entity() != null) &&
                                    ((response.entity().isLive() != null &&
                                            response.entity().isLive().equals(isLive)) &&
                                            (response.entity().isArchived() != null &&
                                                    response.entity().isArchived()
                                                            .equals(archive)));
                        } catch (NotFoundException e) {
                            return false;
                        }
                    });

            return true;
        } catch (ConditionTimeoutException ex) {
            return false;
        }
    }

    /**
     * Retrieves a site by its name.
     *
     * @param siteName The name of the site.
     * @return The ResponseEntityView containing the SiteView object representing the site.
     */
    @ActivateRequestContext
    public ResponseEntityView<SiteView> findSiteByName(final String siteName) {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // Execute the REST call to retrieve folder contents
        return siteAPI.findByName(
                GetSiteByNameRequest.builder().siteName(siteName).build()
        );
    }

}
