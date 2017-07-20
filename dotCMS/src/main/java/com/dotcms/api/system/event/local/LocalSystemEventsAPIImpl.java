package com.dotcms.api.system.event.local;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.system.AppContext;
import com.dotcms.util.Delegate;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation.
 * @author
 */
public class LocalSystemEventsAPIImpl implements LocalSystemEventsAPI, Serializable {

    public static final String LOCAL_SYSTEM_EVENTS_THREAD_POOL_SUBMITTER_NAME = "localsystemevents.submmiter";

    private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<EventSubscriber>>  eventSubscriberByEventTypeMap;
    private final DotSubmitter dotSubmitter;
    private final EventSubscriberFinder eventSubscriberFinder;

    public LocalSystemEventsAPIImpl () {

        this(new ConcurrentHashMap<>(), DotConcurrentFactory.getInstance(), new SubscriberAnnotationEventSubscriberFinder());
    }

    @VisibleForTesting
    public LocalSystemEventsAPIImpl(final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<EventSubscriber>> eventSubscriberByEventTypeMap,
                                    final DotConcurrentFactory dotConcurrentFactory,
                                    final EventSubscriberFinder eventSubscriberFinder) {

        this.eventSubscriberByEventTypeMap = eventSubscriberByEventTypeMap;
        this.dotSubmitter                  = dotConcurrentFactory.getSubmitter(LOCAL_SYSTEM_EVENTS_THREAD_POOL_SUBMITTER_NAME);
        this.eventSubscriberFinder         = eventSubscriberFinder;
    }

    @Override
    public void subscribe(final Object subscriber) {

        final  Map<Class<?>, EventSubscriber> subscriberMap =
                this.eventSubscriberFinder.findSubscriber(subscriber);

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
    public void unsubscribe(final Class<?> eventType) {

        if (null != this.getEventSubscribersByEventType(eventType)) {

            this.eventSubscriberByEventTypeMap.remove(eventType);
        } else {

            Logger.info(this, "Not events for type: " + eventType + " subscribed");
        }
    } // unsubscribe.

    @Override
    public void unsubscribe(final Class<?> eventType, final String subscriberId) {

        final CopyOnWriteArrayList<EventSubscriber> eventSubscribers =
                this.getEventSubscribersByEventType(eventType);
        int indexFound = -1;

        if (null == eventSubscribers) {

            for (int i = 0; i < eventSubscribers.size(); ++i) {

                if (null != eventSubscribers.get(i) && null != eventSubscribers.get(i).getId()
                        && eventSubscribers.get(i).getId().equals(subscriberId)) {

                    indexFound = i; break;
                }
            }

            if (-1 != indexFound && indexFound >= 0 && indexFound < eventSubscribers.size()) {

                eventSubscribers.remove(indexFound);
            } else {

                Logger.info(this, "Not events for type: " + eventType + " subscribed");
            }
        } else {

            Logger.info(this, "Not events for type: " + eventType + " subscribed");
        }
    } // unsubscribe.


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

            Logger.info(this, "The Event: " + event + ", has not any subscribers associated");
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
