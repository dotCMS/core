package com.dotcms.api.system.event.local;

import com.dotmarketing.exception.DotRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This implemention wraps a method in order to be called on notify.
 * @author jsanca
 */
public class MethodEventSubscriber implements EventSubscriber {

    private final Method method;
    private final Object subcriber;
    private final String id;

    public MethodEventSubscriber(final Method method,
                                 final Object subcriber) {

        this.method    = method;
        this.subcriber = subcriber;
        this.id        = subcriber.getClass().getName() + "#"
                                + method.getName();
    }

    @Override
    public String getId() {

        return this.id;
    }

    @Override
    public void notify(final Object event) {

        try {

            this.method.invoke(this.subcriber, new Object[] { event });
        } catch (IllegalArgumentException e) {

            throw new DotRuntimeException("Invalid Argument on method: " + this.id + ", for event: " + event, e);
        } catch (IllegalAccessException e) {

            throw new DotRuntimeException("Method is not inaccessible: "  + this.id + ", for event: " + event, e);
        } catch (InvocationTargetException e) {

            throw new DotRuntimeException("Invocation error on method: "  + this.id + ", for event: " + event, e);
        } catch (Exception e) {

            throw new DotRuntimeException("Error on method: "  + this.id + ", for event: " + event, e);
        }
    }

} // E:O:F:MethodEventSubscriber.
