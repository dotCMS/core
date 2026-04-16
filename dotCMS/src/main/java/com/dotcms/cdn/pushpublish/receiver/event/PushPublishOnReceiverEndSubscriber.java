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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This subscriber is in charge of invalidating CDN cache when push-publish completes on receiver.
 */
public class PushPublishOnReceiverEndSubscriber {

    private final boolean live = false;
    private final User user = APILocator.systemUser();
    private final boolean respectFrontendRoles = false;

    @Subscriber
    public void notify(PushPublishEndOnReceiverEvent event) {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final List<PublishQueueElement> publishQueueElementsUnFiltered =
                event.getPublishQueueElements();
        final List<PublishQueueElement> publishQueueElements =
                publishQueueElementsUnFiltered.stream()
                        .filter(pqe -> "contentlet".equals(pqe.getType()))
                        .collect(Collectors.toList());
        final int minimumNumberToPurgeAll =
                Config.getIntProperty("DOT_CDN_MIN_NUM_TO_PURGE_ALL", 250);

        if (null != publishQueueElements) {
            if (publishQueueElements.size() > minimumNumberToPurgeAll) {
                this.invalidateAllSites(contentletAPI, publishQueueElements);
            } else {
                invalidateContentlets(contentletAPI, publishQueueElements);
            }
        }
    }

    private void invalidateContentlets(final ContentletAPI contentletAPI,
            final List<PublishQueueElement> publishQueueElements) {

        final Map<String, DotCDNAPI> dotCDNAPIMap = new HashMap<>();
        Logger.info(this, "Purging selective Contentlets on CDN");

        for (final PublishQueueElement publishQueueElement : publishQueueElements) {

            final String identifier = publishQueueElement.getAsset();
            final long languageId = publishQueueElement.getLanguageId();
            final Contentlet contentlet = Try.of(() -> contentletAPI
                    .findContentletByIdentifier(identifier, live, languageId, user,
                            respectFrontendRoles)).getOrNull();

            if (null != contentlet) {
                final String hostId = contentlet.getHost();
                final Host site = Try.of(
                        () -> APILocator.getHostAPI().find(hostId, user, respectFrontendRoles))
                        .getOrNull();
                if (null != site) {
                    final DotCDNAPI cdnApi =
                            dotCDNAPIMap.computeIfAbsent(hostId, k -> DotCDNAPI.api(site));
                    Logger.debug(this, () -> "Invalidating the contentlet: "
                            + contentlet.getIdentifier());
                    cdnApi.invalidateContentlet(contentlet);
                } else {
                    Logger.debug(this, () -> "Can not Invalidating the contentlet: "
                            + identifier + " b/c could not the host: " + hostId);
                }
            } else {
                Logger.debug(this, () -> "Can not Invalidating the contentlet: "
                        + identifier + " b/c could not find it");
            }
        }
    }

    private void invalidateAllSites(final ContentletAPI contentletAPI,
            final List<PublishQueueElement> publishQueueElements) {

        final Map<String, Host> hostMap = new HashMap<>();
        for (final PublishQueueElement publishQueueElement : publishQueueElements) {

            final String identifier = publishQueueElement.getAsset();
            final long languageId = publishQueueElement.getLanguageId();
            final Contentlet contentlet = Try.of(() -> contentletAPI
                    .findContentletByIdentifier(identifier, live, languageId, user,
                            respectFrontendRoles)).getOrNull();
            if (null != contentlet) {
                final Host site = Try.of(() -> APILocator.getHostAPI()
                        .find(contentlet.getHost(), user, respectFrontendRoles)).getOrNull();
                if (null != site) {
                    hostMap.put(contentlet.getHost(), site);
                }
            }
        }

        final Collection<Host> sites = hostMap.values();
        for (final Host site : sites) {
            final DotCDNAPI cdnApi = DotCDNAPI.api(site);
            Logger.info(this, "Purging all CDN, host: " + site.getHostname());
            final boolean resultInvalidate = cdnApi.invalidateAll();
            Logger.info(this, "Purge result: " + resultInvalidate
                    + " host: " + site.getHostname());
        }
    }
}
