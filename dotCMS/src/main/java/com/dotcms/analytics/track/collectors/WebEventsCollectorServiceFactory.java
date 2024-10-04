package com.dotcms.analytics.track.collectors;

import com.dotcms.analytics.track.matchers.RequestMatcher;
import com.dotcms.jitsu.EventLogRunnable;
import com.dotcms.jitsu.EventLogSubmitter;
import com.dotcms.visitor.filter.characteristics.Character;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.filters.Constants;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * This class provides the default implementation for the WebEventsCollectorService
 *
 * @author jsanca
 */
public class WebEventsCollectorServiceFactory {

    private WebEventsCollectorServiceFactory () {}

    private final WebEventsCollectorService webEventsCollectorService = new WebEventsCollectorServiceImpl();

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

    private static class WebEventsCollectorServiceImpl implements WebEventsCollectorService {

        private final Collectors baseCollectors = new Collectors();
        private final Collectors eventCreatorCollectors = new Collectors();

        private final EventLogSubmitter submitter = new EventLogSubmitter();

        WebEventsCollectorServiceImpl () {

            addCollector(new BasicProfileCollector(), new FilesCollector(), new PagesCollector(),
                    new PageDetailCollector(), new SyncVanitiesCollector(), new AsyncVanitiesCollector());
        }

        @Override
        public void fireCollectors(final HttpServletRequest request,
                                   final HttpServletResponse response,
                                   final RequestMatcher requestMatcher) {

            if (!baseCollectors.isEmpty() || !eventCreatorCollectors.isEmpty()) {

                this.fireCollectorsAndEmitEvent(request, response, requestMatcher);
            } else {

                Logger.debug(this, ()-> "No collectors to run");
            }
        }

        private void fireCollectorsAndEmitEvent(final HttpServletRequest request,
                                                final HttpServletResponse response,
                                                final RequestMatcher requestMatcher) {

            final Character character = WebAPILocator.getCharacterWebAPI().getOrCreateCharacter(request, response);
            final Host site = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request);

            final CollectorPayloadBean base = new ConcurrentCollectorPayloadBean();
            final CollectorContextMap syncCollectorContextMap =
                    new RequestCharacterCollectorContextMap(request, character, requestMatcher);

            Logger.debug(this, ()-> "Running sync collectors");

            collect(baseCollectors.syncCollectors.values(), base, syncCollectorContextMap);
            final List<CollectorPayloadBean> futureEvents = getFutureEvents(eventCreatorCollectors.syncCollectors.values(),
                    syncCollectorContextMap);


            // if there is anything to run async
            final PageMode pageMode = PageMode.get(request);
            final CollectorContextMap collectorContextMap = new CharacterCollectorContextMap(character, requestMatcher,
                    getCollectorContextMap(request, pageMode, site));

            try {
                this.submitter.logEvent(
                        new EventLogRunnable(site, () -> {
                            Logger.debug(this, () -> "Running async collectors");

                            collect(baseCollectors.asyncCollectors.values(), base, collectorContextMap);
                            final List<CollectorPayloadBean> asyncFutureEvents = getFutureEvents(
                                    eventCreatorCollectors.asyncCollectors.values(), collectorContextMap);

                            return Stream.concat(futureEvents.stream(), asyncFutureEvents.stream())
                                    .map(payload -> payload.add(base))
                                    .map(CollectorPayloadBean::toMap)
                                    .collect(java.util.stream.Collectors.toList());
                        }));
            } catch (Exception e) {
                Logger.debug(WebEventsCollectorServiceFactory.class, () -> "Error saving Analytics Events:" + e.getMessage());
            }
        }

        private static Map<String, Object> getCollectorContextMap(final HttpServletRequest request,
                                                                  final PageMode pageMode, final Host site) {
            final Map<String, Object> contextMap = new HashMap<>(Map.of("uri", request.getRequestURI(),
                    "pageMode", pageMode,
                    "currentHost", site,
                    "requestId", request.getAttribute("requestId")));

            if (Objects.nonNull(request.getAttribute(Constants.VANITY_URL_OBJECT))) {
                contextMap.put(Constants.VANITY_URL_OBJECT, request.getAttribute(Constants.VANITY_URL_OBJECT));
            }

            return contextMap;
        }

        private List<CollectorPayloadBean> getFutureEvents(final Collection<Collector> eventCreators,
                                                           final CollectorContextMap collectorContextMap) {
           return eventCreators.stream()
                    .filter(collector -> collector.test(collectorContextMap))
                    .map(collector -> {
                        final CollectorPayloadBean futureEvent = new ConcurrentCollectorPayloadBean();
                        collector.collect(collectorContextMap, futureEvent);
                        return futureEvent;
                    }).collect(java.util.stream.Collectors.toList());
        }

        private void collect(final Collection<Collector> collectors,
                                           final CollectorPayloadBean payload,
                                           final CollectorContextMap syncCollectorContextMap) {
            collectors.stream()
                    .filter(collector -> collector.test(syncCollectorContextMap))
                    .forEach(collector -> collector.collect(syncCollectorContextMap, payload));
        }

        @Override
        public void addCollector(final Collector... collectors) {
            for (final Collector collector : collectors) {
                if (collector.isEventCreator()) {
                    eventCreatorCollectors.add(collector);
                } else {
                    baseCollectors.add(collector);
                }
            }
        }

        @Override
        public void removeCollector(final String collectorId) {
            eventCreatorCollectors.remove(collectorId);
            baseCollectors.remove(collectorId);
        }

        private static class Collectors {
            private final Map<String, Collector> syncCollectors  = new ConcurrentHashMap<>();
            private final Map<String, Collector> asyncCollectors = new ConcurrentHashMap<>();

            public void add(final Collector... collectors){
                for (final Collector collector : collectors) {
                    if (collector.isAsync()) {
                        asyncCollectors.put(collector.getId(), collector);
                    } else {
                        syncCollectors.put(collector.getId(), collector);
                    }
                }
            }

            public void remove(String collectorId) {
                asyncCollectors.remove(collectorId);
                syncCollectors.remove(collectorId);
            }

            public boolean isEmpty() {
                return asyncCollectors.isEmpty() && !syncCollectors.isEmpty();
            }
        }
    }

}
