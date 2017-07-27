package com.dotcms.system.event.local.business;

import com.dotcms.system.event.local.model.DefaultOrphanEventSubscriber;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.system.event.local.type.OrphanEvent;
import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation.
 * The implementation will be provided by {@link LocalSystemEventsAPIFactory}
 * @author
 */
class LocalSystemEventsAPIImpl implements LocalSystemEventsAPI {

    public static final String LOCAL_SYSTEM_EVENTS_THREAD_POOL_SUBMITTER_NAME = "localsystemevents.submmiter";
    public static final String NO_ANY_SUBSCRIBERS_ASSOCIATED_TO_THE_EVENT_TYPE = "No subscriber associated to the event type: ";

    private final ConcurrentMap<Class<?>, CopyOnWriteArrayList<EventSubscriber>>  eventSubscriberByEventTypeMap;
    private final DotSubmitter dotSubmitter;
    private final EventSubscriberFinder eventSubscriberFinder;
    private volatile EventSubscriber<OrphanEvent> orphanEventSubscriber;

    public LocalSystemEventsAPIImpl () {

        this(new ConcurrentHashMap<>(), DotConcurrentFactory.getInstance(),
                new SubscriberAnnotationEventSubscriberFinder(),
                new DefaultOrphanEventSubscriber());
    }

    @VisibleForTesting
    public LocalSystemEventsAPIImpl(final ConcurrentMap<Class<?>, CopyOnWriteArrayList<EventSubscriber>> eventSubscriberByEventTypeMap,
                                    final DotConcurrentFactory dotConcurrentFactory,
                                    final EventSubscriberFinder eventSubscriberFinder,
                                    final EventSubscriber<OrphanEvent> orphanEventSubscriber) {

        this.eventSubscriberByEventTypeMap = eventSubscriberByEventTypeMap;
        this.dotSubmitter                  = dotConcurrentFactory.getSubmitter(LOCAL_SYSTEM_EVENTS_THREAD_POOL_SUBMITTER_NAME);
        this.eventSubscriberFinder         = eventSubscriberFinder;
        this.orphanEventSubscriber         = orphanEventSubscriber;
    }

    @Override
    public void subscribe(final Object subscriber) {

        final  Map<Class<?>, EventSubscriber> subscriberMap =
                this.eventSubscriberFinder.findSubscribers(subscriber);

        if (null != subscriberMap && !subscriberMap.isEmpty()) {

            subscriberMap.forEach((eventType, eventSubscriber) -> this.subscribe(eventType, eventSubscriber));
        }
    } // subscribe.

    @Override
    public void subscribe(final Class<?> eventType, final EventSubscriber subscriber) {

        CopyOnWriteArrayList<EventSubscriber> eventSubscribers =
                this.getEventSubscribersByEventType(eventType);

        if (null == eventSubscribers) {

            synchronized (this) {

                eventSubscribers =
                        this.getEventSubscribersByEventType(eventType);

                if (null == eventSubscribers) {

                    eventSubscribers = new CopyOnWriteArrayList<>();
                    this.eventSubscriberByEventTypeMap.put(eventType, eventSubscribers);
                }
            }
        }

        eventSubscribers.add(subscriber);
    } // subscribe.

    @Override
    public boolean unsubscribe(final Class<?> eventType) {

        if (null != this.getEventSubscribersByEventType(eventType)) {

            return null != this.eventSubscriberByEventTypeMap.remove(eventType);
        } else {

            Logger.info(this, NO_ANY_SUBSCRIBERS_ASSOCIATED_TO_THE_EVENT_TYPE + eventType);
        }

        return false;
    } // unsubscribe.

    @Override
    public boolean unsubscribe(final Object subscriber) {

        final AtomicBoolean unsubscribed = new AtomicBoolean(true);
        final  Map<Class<?>, EventSubscriber> subscriberMap =
                this.eventSubscriberFinder.findSubscribers(subscriber);

        if (null != subscriberMap && !subscriberMap.isEmpty()) {

            subscriberMap.forEach((eventType, eventSubscriber) ->
                    unsubscribed
                            .set(this.unsubscribe(eventType, eventSubscriber.getId())
                                    && unsubscribed
                                    .get()));
        }

        return unsubscribed.get();
    }

    @Override
    public boolean unsubscribe(final Class<?> eventType, final String subscriberId) {

        final CopyOnWriteArrayList<EventSubscriber> eventSubscribers =
                this.getEventSubscribersByEventType(eventType);
        int indexFound = -1;

        if (null != eventSubscribers) {

            for (int i = 0; i < eventSubscribers.size(); ++i) {

                if (null != eventSubscribers.get(i) && null != eventSubscribers.get(i).getId()
                        && eventSubscribers.get(i).getId().equals(subscriberId)) {

                    indexFound = i; break;
                }
            }

            if (-1 != indexFound && indexFound >= 0 && indexFound < eventSubscribers.size()) {

                return null != eventSubscribers.remove(indexFound);
            } else {

                Logger.info(this, NO_ANY_SUBSCRIBERS_ASSOCIATED_TO_THE_EVENT_TYPE
                        + eventType + " and id: " + subscriberId);
            }
        } else {

            Logger.info(this, NO_ANY_SUBSCRIBERS_ASSOCIATED_TO_THE_EVENT_TYPE + eventType);
        }

        return false;
    } // unsubscribe.

    @Override
    public EventSubscriber findSubscriber (final Class<?> eventType, String subscriberId) {

        final CopyOnWriteArrayList<EventSubscriber> eventSubscribers =
                this.getEventSubscribersByEventType(eventType);

        if (null != eventSubscribers) {

            for (int i = 0; i < eventSubscribers.size(); ++i) {

                if (null != eventSubscribers.get(i) && null != eventSubscribers.get(i).getId()
                        && eventSubscribers.get(i).getId().equals(subscriberId)) {

                    return eventSubscribers.get(i);
                }
            }
        }

        return null;
    } // findSubscriber.

    @Override
    public void setOrphanEventSubscriber(final EventSubscriber<OrphanEvent> orphanEventSubscriber) {

        if (null != orphanEventSubscriber) {

            synchronized (this) {

                this.orphanEventSubscriber = orphanEventSubscriber;
            }
        }
    } // setOrphanEventSubscriber.


    @Override
    public void asyncNotify(final Object event) {

        this.dotSubmitter.submit(()-> this.notify(event));
    } // asyncNotify.


    @Override
    public void notify(final Object event) {

        final CopyOnWriteArrayList<EventSubscriber> eventSubscribers =
                this.getEventSubscribersByEventType(event.getClass());

        if (null != eventSubscribers) {

            for (EventSubscriber eventSubscriber : eventSubscribers) {

                if (null != eventSubscriber) {

                    eventSubscriber.notify(event);
                }
            }
        } else {

            this.orphanEventSubscriber.notify(new OrphanEvent(event));
        }
    } // asyncNotify.

    protected CopyOnWriteArrayList<EventSubscriber> getEventSubscribersByEventType (final Class<?> eventType) {

        CopyOnWriteArrayList<EventSubscriber> eventSubscribers = null;

        try {

            eventSubscribers = this.eventSubscriberByEventTypeMap.get(eventType);
        } catch (Exception e) {
            Logger.error(this, "Local System Events: " + e.getMessage(), e);
        }

        return eventSubscribers;
    } // getEventSubscribersByEventType.
} // E:O:F:LocalSystemEventsAPIImpl.
