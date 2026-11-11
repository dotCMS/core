package com.dotcms.cdn.pushpublish.receiver.event;

import com.dotcms.cdn.api.DotCDNAPI;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.system.event.local.model.Subscriber;
import com.dotcms.system.event.local.type.pushpublish.receiver.PushPublishEndOnReceiverEvent;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.control.Try;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This subscriber is in charge of invalidating CDN cache when push-publish completes on receiver.
 */
public class PushPublishOnReceiverEndSubscriber {

    private static final boolean LIVE = false;
    private static final boolean RESPECT_FRONTEND_ROLES = false;

    private final User user = APILocator.systemUser();

    @Subscriber
    public void notify(PushPublishEndOnReceiverEvent event) {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final List<PublishQueueElement> publishQueueElements =
                event.getPublishQueueElements().stream()
                        .filter(pqe -> "contentlet".equals(pqe.getType()))
                        .collect(Collectors.toList());
        final int minimumNumberToPurgeAll =
                Config.getIntProperty("DOT_CDN_MIN_NUM_TO_PURGE_ALL", 250);

        if (publishQueueElements.size() > minimumNumberToPurgeAll) {
            this.invalidateAllSites(contentletAPI, publishQueueElements);
        } else {
            invalidateContentlets(contentletAPI, publishQueueElements);
        }
    }

    private void invalidateContentlets(final ContentletAPI contentletAPI,
            final List<PublishQueueElement> publishQueueElements) {

        final Map<String, DotCDNAPI> dotCDNAPIMap = new HashMap<>();
        final Set<String> unconfiguredHosts = new HashSet<>();
        Logger.info(this, "Purging selective Contentlets on CDN");

        for (final PublishQueueElement publishQueueElement : publishQueueElements) {

            final String identifier = publishQueueElement.getAsset();
            final Contentlet contentlet =
                    resolveContentlet(contentletAPI, user, publishQueueElement);

            if (null == contentlet) {
                Logger.debug(this, () -> "Cannot invalidate contentlet: "
                        + identifier + " — not found");
                continue;
            }

            final String hostId = contentlet.getHost();
            if (unconfiguredHosts.contains(hostId)) {
                continue;
            }

            final Host site = Try.of(
                    () -> APILocator.getHostAPI().find(hostId, user, RESPECT_FRONTEND_ROLES))
                    .getOrNull();
            if (null == site) {
                Logger.debug(this, () -> "Cannot invalidate contentlet: "
                        + identifier + " — host not found: " + hostId);
                continue;
            }

            if (!DotCDNAPI.isConfigured(site)) {
                unconfiguredHosts.add(hostId);
                Logger.debug(this, () -> "dotCDN not configured for host: "
                        + site.getHostname() + ", skipping");
                continue;
            }

            final DotCDNAPI cdnApi =
                    dotCDNAPIMap.computeIfAbsent(hostId, k -> DotCDNAPI.api(site));
            Logger.debug(this, () -> "Invalidating contentlet: "
                    + contentlet.getIdentifier());
            Try.run(() -> cdnApi.invalidateContentlet(contentlet))
                    .onFailure(e -> Logger.warn(this,
                            "CDN purge failed for contentlet: " + contentlet.getIdentifier()
                                    + " - " + e.getMessage()));
        }
    }

    /**
     * Finds the contentlet a received queue element points at. Elements read from a bundle
     * manifest carry the contentlet's inode and no language, while elements from a legacy
     * {@code bundle.xml} carry an identifier and a language, so each shape is looked up its own way.
     *
     * @param contentletAPI the API used for the lookup
     * @param user the user the lookup runs as
     * @param publishQueueElement the received element
     * @return the contentlet, or {@code null} when it cannot be found
     */
    static Contentlet resolveContentlet(final ContentletAPI contentletAPI, final User user,
            final PublishQueueElement publishQueueElement) {

        final String asset = publishQueueElement.getAsset();
        final Integer languageId = publishQueueElement.getLanguageId();
        return Try.of(() -> null == languageId
                ? contentletAPI.find(asset, user, RESPECT_FRONTEND_ROLES)
                : contentletAPI.findContentletByIdentifier(asset, LIVE, languageId, user,
                        RESPECT_FRONTEND_ROLES)).getOrNull();
    }

    private void invalidateAllSites(final ContentletAPI contentletAPI,
            final List<PublishQueueElement> publishQueueElements) {

        final Map<String, Host> hostMap = new HashMap<>();
        for (final PublishQueueElement publishQueueElement : publishQueueElements) {

            final Contentlet contentlet =
                    resolveContentlet(contentletAPI, user, publishQueueElement);
            if (null != contentlet) {
                final Host site = Try.of(() -> APILocator.getHostAPI()
                        .find(contentlet.getHost(), user, RESPECT_FRONTEND_ROLES)).getOrNull();
                if (null != site) {
                    hostMap.put(contentlet.getHost(), site);
                }
            }
        }

        for (final Host site : hostMap.values()) {
            if (!DotCDNAPI.isConfigured(site)) {
                Logger.debug(this, () -> "dotCDN not configured for host: "
                        + site.getHostname() + ", skipping purge-all");
                continue;
            }

            final DotCDNAPI cdnApi = DotCDNAPI.api(site);
            Logger.info(this, "Purging all CDN, host: " + site.getHostname());
            Try.of(cdnApi::invalidateAll)
                    .onSuccess(resultInvalidate -> Logger.info(this, "Purge result: "
                            + resultInvalidate + " host: " + site.getHostname()))
                    .onFailure(e -> Logger.warn(this, "Unable to purge all CDN, host: "
                            + site.getHostname() + " - " + e.getMessage()));
        }
    }
}
