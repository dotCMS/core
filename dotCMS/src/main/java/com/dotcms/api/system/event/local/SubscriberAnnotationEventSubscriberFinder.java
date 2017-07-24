package com.dotcms.api.system.event.local;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static com.dotcms.util.AnnotationUtils.getMethodsAnnotatedBy;

/**
 * A finder will look for all method annotated by @{@link Subscriber} with just one parameter on the method and
 * subscribe a {@link EventSubscriber} with the ClassName#MethodName as id.
 * Be aware the name method subscriber should unique on the class.
 * @author jsanca
 */
public class SubscriberAnnotationEventSubscriberFinder implements EventSubscriberFinder {

    /**
     * Finds all methods annotated by {@link Subscriber} in order to create a default {@link EventSubscriber} for it.
     * The id will be ClassName#MethodName
     *
     * @param subcriber  object whose handlers are desired.
     * @return Map of Class type (event type) associated to the {@link EventSubscriber}
     *
     * @throws IllegalArgumentException if {@code source} is not appropriate for
     *         this strategy (in ways that this interface does not define).
     */
    @Override
    public Map<Class<?>, EventSubscriber> findSubscribers(final Object subcriber) {

        final ImmutableMap.Builder<Class<?>, EventSubscriber> eventTypeSubscriberMap =
                new ImmutableMap.Builder<>();
        final Set<Method> annotatedMethods =
                getMethodsAnnotatedBy(subcriber, Subscriber.class);
        Class<?>[] parameterTypes = null;

        if (annotatedMethods != null) {
            for (Method method : annotatedMethods) {

                if (method != null) {

                    parameterTypes = method.getParameterTypes();
                    if (1 != parameterTypes.length) {

                        throw new IllegalArgumentException(
                                "A method annotated by Subscriber must have just one argument (the event type), see " +
                                        subcriber.getClass() + method);
                    }

                    if (method.isAccessible()) {

                        throw new IllegalArgumentException(
                                "A method annotated by Subscriber must accessible in order to be called, see " +
                                        subcriber.getClass() + method);
                    }

                    eventTypeSubscriberMap.put(parameterTypes[0],
                            new MethodEventSubscriber (method, subcriber));
                }
            }
        }

        return eventTypeSubscriberMap.build();
    } // findAllHandlers.

} // E:O:F:SubscriberAnnotationEventSubscriberFinder.
