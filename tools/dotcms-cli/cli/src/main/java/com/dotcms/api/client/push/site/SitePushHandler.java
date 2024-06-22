package com.dotcms.api.client.push.site;

import static com.dotcms.cli.command.site.SitePush.SITE_PUSH_OPTION_FORCE_EXECUTION;

import com.dotcms.api.SiteAPI;
import com.dotcms.api.client.model.RestClientFactory;
import com.dotcms.api.client.push.PushHandler;
import com.dotcms.api.client.util.NamingUtils;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.site.CreateUpdateSiteRequest;
import com.dotcms.model.site.GetSiteByNameRequest;
import com.dotcms.model.site.SiteView;
import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import org.apache.commons.lang3.BooleanUtils;
import org.jboss.logging.Logger;

@Dependent
public class SitePushHandler implements PushHandler<SiteView> {

    @Inject
    protected RestClientFactory clientFactory;

    @Inject
    Logger logger;

    @Override
    public Class<SiteView> type() {
        return SiteView.class;
    }

    @Override
    public String title() {
        return "Sites";
    }

    @Override
    public String fileName(final SiteView site) {
        return NamingUtils.siteFileName(site);
    }

    @Override
    public String contentSimpleDisplay(SiteView site) {
        return String.format(
                "name: [%s] id: [%s] inode: [%s] live:[%s] default: [%s] archived: [%s]",
                site.hostName(),
                site.identifier(),
                site.inode(),
                BooleanUtils.toStringYesNo(site.isLive()),
                BooleanUtils.toStringYesNo(site.isDefault()),
                BooleanUtils.toStringYesNo(site.isArchived())
        );
    }

    @ActivateRequestContext
    @Override
    public SiteView add(File localFile, SiteView localSite, Map<String, Object> customOptions) {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        // Creating the site
        var response = siteAPI.create(
                toRequest(localSite, customOptions)
        );

        // Publishing the site
        if (Boolean.TRUE.equals(localSite.isLive())) {
            response = siteAPI.publish(response.entity().identifier());
        }

        return response.entity();
    }

    @ActivateRequestContext
    @Override
    public SiteView edit(File localFile, SiteView localSite, SiteView serverSite,
            Map<String, Object> customOptions) {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        ResponseEntityView<SiteView> response;

        // Unarchiving the site if necessary, this is necessary because the site API doesn't allow
        //  updating an archived site
        if (Boolean.TRUE.equals(serverSite.isArchived())) {
            siteAPI.unarchive(localSite.identifier());
        }

        response = siteAPI.update(
                localSite.identifier(),
                toRequest(localSite, customOptions)
        );
        var siteView = response.entity();

        if (shouldPublishSite(localSite, serverSite)) {
            return handleSitePublishing(siteAPI, localSite);
        }

        if (shouldArchiveSite(localSite, serverSite)) {
            return handleSiteArchiving(siteAPI, localSite);
        }

        if (shouldUnpublishSite(localSite, serverSite)) {
            return handleSiteUnpublishing(siteAPI, localSite);
        }

        return siteView;
    }

    @ActivateRequestContext
    @Override
    public void remove(SiteView serverSite, Map<String, Object> customOptions) {

        final SiteAPI siteAPI = clientFactory.getClient(SiteAPI.class);

        siteAPI.archive(
                serverSite.identifier()
        );

        siteAPI.delete(
                serverSite.identifier()
        );
    }

    CreateUpdateSiteRequest toRequest(final SiteView siteView,
            final Map<String, Object> customOptions) {

        var forceExecution = false;
        if (customOptions != null && customOptions.containsKey(SITE_PUSH_OPTION_FORCE_EXECUTION)) {
            forceExecution = (boolean) customOptions.get(SITE_PUSH_OPTION_FORCE_EXECUTION);
        }

        return CreateUpdateSiteRequest.builder()
                .siteName(siteView.siteName())
                .keywords(siteView.keywords())
                .googleMap(siteView.googleMap())
                .addThis(siteView.addThis())
                .aliases(siteView.aliases())
                .identifier(siteView.identifier())
                .inode(siteView.inode())
                .proxyUrlForEditMode(siteView.proxyUrlForEditMode())
                .googleAnalytics(siteView.googleAnalytics())
                .description(siteView.description())
                .tagStorage(siteView.tagStorage())
                .siteThumbnail(siteView.siteThumbnail())
                .embeddedDashboard(siteView.embeddedDashboard())
                .forceExecution(forceExecution)
                .isDefault(Boolean.TRUE.equals(siteView.isDefault()))
                .variables(siteView.variables())
                .build();
    }

    /**
     * Determines whether a site should be published based on its local and server versions.
     *
     * @param localSite  The SiteView object representing the local version of the site.
     * @param serverSite The SiteView object representing the server version of the site.
     * @return True if the local site is live and the server site is not live, false otherwise.
     */
    private boolean shouldPublishSite(SiteView localSite, SiteView serverSite) {
        return Boolean.TRUE.equals(localSite.isLive()) &&
                Boolean.FALSE.equals(serverSite.isLive());
    }

    /**
     * Determines whether a site should be archived based on its local and server versions.
     *
     * @param localSite  The SiteView object representing the local version of the site.
     * @param serverSite The SiteView object representing the server version of the site.
     * @return True if the local site is archived and the server site is not archived, false
     * otherwise.
     */
    private boolean shouldArchiveSite(SiteView localSite, SiteView serverSite) {
        return Boolean.TRUE.equals(localSite.isArchived()) &&
                Boolean.FALSE.equals(serverSite.isArchived());
    }

    /**
     * Determines whether a site should be unpublished based on its local and server versions.
     *
     * @param localSite  The SiteView object representing the local version of the site.
     * @param serverSite The SiteView object representing the server version of the site.
     * @return True if the local site is not live and the server site is live, false otherwise.
     */
    private boolean shouldUnpublishSite(SiteView localSite, SiteView serverSite) {
        return Boolean.FALSE.equals(localSite.isLive()) &&
                Boolean.TRUE.equals(serverSite.isLive());
    }

    /**
     * Handles the site publishing process.
     *
     * @param siteAPI   The SiteAPI instance used to interact with the DotCMS API.
     * @param localSite The local SiteView object representing the site to be published.
     * @return The SiteView object representing the published site.
     */
    private SiteView handleSitePublishing(SiteAPI siteAPI, SiteView localSite) {

        // Publishing the site
        final var response = siteAPI.publish(localSite.identifier());
        var siteView = response.entity();

        if (response.entity() == null || Boolean.FALSE.equals(response.entity().isLive())) {

            var siteViewResponse = verifyAndReturnSiteAfterCompletion(
                    "published",
                    localSite.siteName(),
                    true,
                    false);
            if (siteViewResponse != null) {
                siteView = siteViewResponse;
            }
        }

        return siteView;
    }

    /**
     * Handles the site archiving process.
     *
     * @param siteAPI   The SiteAPI instance used to interact with the DotCMS API.
     * @param localSite The local SiteView object representing the site to be archived.
     * @return The SiteView object representing the archived site.
     */
    private SiteView handleSiteArchiving(SiteAPI siteAPI, SiteView localSite) {

        // Archiving the site
        final var response = siteAPI.archive(localSite.identifier());
        var siteView = response.entity();

        if (response.entity() == null || Boolean.FALSE.equals(response.entity().isArchived())) {

            final var siteViewResponse = verifyAndReturnSiteAfterCompletion(
                    "archived",
                    localSite.siteName(),
                    false,
                    true
            );

            if (siteViewResponse != null) {
                siteView = siteViewResponse;
            }
        }

        return siteView;
    }

    /**
     * Handles the process of unpublishing a site.
     *
     * @param siteAPI   The SiteAPI instance used to interact with the DotCMS API.
     * @param localSite The local SiteView object representing the site to be unpublished.
     * @return The SiteView object representing the unpublished site.
     */
    private SiteView handleSiteUnpublishing(SiteAPI siteAPI, SiteView localSite) {

        // Unpublishing the site
        final var response = siteAPI.unpublish(localSite.identifier());
        var siteView = response.entity();

        if (response.entity() == null || Boolean.TRUE.equals(response.entity().isLive())) {

            final var siteViewResponse = verifyAndReturnSiteAfterCompletion(
                    "unpublished",
                    localSite.siteName(),
                    false,
                    false
            );

            if (siteViewResponse != null) {
                siteView = siteViewResponse;
            }
        }

        return siteView;
    }

    /**
     * Fallback method to return the latest site view after a status changes operation is completed,
     * this is required because the site API could return an entity that does not reflect the latest
     * status of the site as it depends on the indexing process.
     * <p>
     * Most of the time this call won't be necessary as the site API will return the latest site.
     *
     * @param siteName   the site name
     * @param isSiteLive whether the site is live
     * @param isArchived whether the site is archived
     * @return The site view or null if the site could not be retrieved
     */
    private SiteView verifyAndReturnSiteAfterCompletion(final String operation,
            final String siteName, final boolean isSiteLive,
            final boolean isArchived) {

        var siteViewFuture = verifyAndReturnSiteAfterCompletion(
                siteName,
                isSiteLive,
                isArchived
        );
        final var siteViewResponse = siteViewFuture.exceptionally(ex -> null).join();

        if (siteViewResponse == null) {
            logger.error(
                    String.format("Unable to retrieve %s site", operation)
            );
        }

        return siteViewResponse;
    }

    /**
     * Fallback method to return the latest site view after a status changes operation is completed,
     * this is required because the site API could return an entity that does not reflect the latest
     * status of the site as it depends on the indexing process.
     * <p>
     * Most of the time this call won't be necessary as the site API will return the latest site.
     *
     * @param siteName   the site name
     * @param isSiteLive whether the site is live
     * @param isArchived whether the site is archived
     * @return A completable future with the site view
     */
    @ActivateRequestContext
    CompletableFuture<SiteView> verifyAndReturnSiteAfterCompletion(
            final String siteName, final boolean isSiteLive, final boolean isArchived
    ) {

        // Using a single thread pool which will schedule the polling
        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        CompletableFuture<SiteView> future = new CompletableFuture<>();

        // The task we are scheduling (status polling)
        Runnable task = new Runnable() {

            final long start = System.currentTimeMillis();
            final long end = start + (15 * 1000);

            @Override
            public void run() {

                if (System.currentTimeMillis() < end) {
                    try {
                        var response = findSiteByName(siteName);
                        if ((response != null && response.entity() != null) &&
                                ((response.entity().isLive() != null &&
                                        response.entity().isLive().equals(isSiteLive)) &&
                                        (response.entity().isArchived() != null &&
                                                response.entity().isArchived().equals(isArchived)))
                        ) {

                            // Complete the future with the site view
                            future.complete(response.entity());
                            scheduler.shutdown(); // No more tasks after successful polling
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                        scheduler.shutdown();  // No more tasks on error
                    }
                } else {
                    future.completeExceptionally(
                            new TimeoutException("Timeout when polling site status")
                    );
                    scheduler.shutdown();  // No more tasks when polling ends
                }
            }
        };

        // Schedule the task to run every 2 seconds
        final ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(
                task,
                0,
                2,
                TimeUnit.SECONDS
        );

        // If future was completed exceptionally, cancel the polling
        future.exceptionally(thr -> {
            logger.debug(thr.getMessage(), thr);
            scheduledFuture.cancel(true);
            return null;
        });

        return future;
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