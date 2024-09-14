package com.dotcms.api.client.push.site;

import com.dotcms.model.site.SiteView;
import jakarta.enterprise.context.control.ActivateRequestContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

class VerifyAndReturnSiteRunnable implements Runnable {

    private final SitePushHandler sitePushHandler;
    private final long start = System.currentTimeMillis();
    private final long end = start + (15 * 1000);
    private final CompletableFuture<SiteView> future;
    private final String siteName;
    private final boolean isSiteLive;
    private final boolean isArchived;
    private final ScheduledExecutorService scheduler;

    public VerifyAndReturnSiteRunnable(SitePushHandler sitePushHandler, final CompletableFuture<SiteView> future,
                                       final String siteName,
                                       final boolean isSiteLive,
                                       final boolean isArchived,
                                       final ScheduledExecutorService scheduler) {
        this.sitePushHandler = sitePushHandler;
        this.future = future;
        this.siteName = siteName;
        this.isSiteLive = isSiteLive;
        this.isArchived = isArchived;
        this.scheduler = scheduler;
    }

    @Override
    @ActivateRequestContext
    public void run() {
        if (System.currentTimeMillis() < end) {
            try {
                var response = sitePushHandler.findSiteByName(siteName);
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
}
