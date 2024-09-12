package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.jitsu.EventLogRunnable;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotcms.visitor.filter.characteristics.Character;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import org.apache.commons.lang3.mutable.MutableObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class provides the default implementation for the WebEventsCollectorService
 *
 * @author jsanca
 */
public class WebEventsCollectorServiceFactory {

    private WebEventsCollectorService webEventsCollectorService = new WebEventsCollectorServiceImpl();

    private static class SingletonHolder {
        private static final WebEventsCollectorServiceFactory INSTANCE = new WebEventsCollectorServiceFactory();
    }

    /**
     * Get the instance.
     * @return WebEventsCollectorServiceFactory
     */
    public static WebEventsCollectorServiceFactory getInstance() {

        return WebEventsCollectorServiceFactory.SingletonHolder.INSTANCE;
    } // getInstance.


    public WebEventsCollectorService getWebEventsCollectorService() {

        return webEventsCollectorService;
    }

    private class WebEventsCollectorServiceImpl implements WebEventsCollectorService {

        private final Map<String, Collector> syncCollectors  = new ConcurrentHashMap<>();
        private final Map<String, Collector> asyncCollectors = new ConcurrentHashMap<>();
        private final EventLogSubmitter submitter = new EventLogSubmitter();

        WebEventsCollectorServiceImpl () {

            addCollector(new BasicProfileCollector(), new FilesCollector(), new PagesCollector(),
                    new SyncVanitiesCollector(), new AsyncVanitiesCollector());
        }

        @Override
        public void fireCollectors(final HttpServletRequest request,
                                   final HttpServletResponse response,
                                   final RequestMatcher requestMatcher) {

            if (!asyncCollectors.isEmpty() || !syncCollectors.isEmpty()) {

                this.fireCollectorsAndEmitEvent(request, response, requestMatcher);
            } else {

                Logger.debug(this, ()-> "No collectors to ran");
            }
        }

        private void fireCollectorsAndEmitEvent(final HttpServletRequest request,
                                                final HttpServletResponse response,
                                                final RequestMatcher requestMatcher) {

            final Character character = WebAPILocator.getCharacterWebAPI().getOrCreateCharacter(request, response);
            final Host site = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);
            final MutableObject<CollectionCollectorPayloadBean> collectorPayloadBeanMutableObject = new MutableObject<>(new ConcurrentCollectionCollectorPayloadBean());
            if (!syncCollectors.isEmpty()) {

                Logger.debug(this, ()-> "Running sync collectors");
                final CollectorContextMap syncCollectorContextMap = new RequestCharacterCollectorContextMap(request, character, requestMatcher);
                // we collect info which is sync and includes the request.
                syncCollectors.values().stream().filter(collector -> collector.test(syncCollectorContextMap))
                        .forEach(collector -> collectorPayloadBeanMutableObject
                                .setValue(collector.collect(syncCollectorContextMap, collectorPayloadBeanMutableObject.getValue())));
            }

            // if there is anything to run async
            final PageMode pageMode = PageMode.get(request);
            final CollectorContextMap collectorContextMap = new CharacterCollectorContextMap(character, requestMatcher,
                    Map.of("uri", request.getRequestURI(),
                            "pageMode", pageMode,
                            "siteId", site.getIdentifier(),
                            "requestId", request.getAttribute("requestId")));
            this.submitter.logEvent(
                    new EventLogRunnable(site, ()-> {
                        Logger.debug(this, ()-> "Running async collectors");
                        asyncCollectors.values().stream()
                                .filter(collector -> collector.test(collectorContextMap))
                                .forEach(collector -> { collectorPayloadBeanMutableObject.setValue(
                                        collector.collect(collectorContextMap, collectorPayloadBeanMutableObject.getValue())); });
                        return collectorPayloadBeanMutableObject.getValue().getCollection().stream().map(CollectorPayloadBean::toMap).collect(Collectors.toList());
                    }));

        }

        @Override
        public void addCollector(final Collector... collectors) {
            for (final Collector collector : collectors) {
                if (collector.isAsync()) {

                    asyncCollectors.put(collector.getId(), collector);
                } else {
                    syncCollectors.put(collector.getId(), collector);
                }
            }
        }

        @Override
        public void removeCollector(final String collectorId) {
            if (syncCollectors.containsKey(collectorId)) {
                syncCollectors.remove(collectorId);
            }

            if (asyncCollectors.containsKey(collectorId)) {
                asyncCollectors.remove(collectorId);
            }
        }
    }
}
