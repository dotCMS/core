package com.dotcms.content.model.annotation;

import com.dotcms.content.model.hydration.HydrationDelegate;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * When used in conjunction with {@link Hydration}
 * This basically instructs our framework what property we desire to inject and what implementation of {@link HydrationDelegate} must be used
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface HydrateWith {

    /**
     * This is the class that knows how calculate the value we are about to inject in the property specified by method propertyName
     * @return
     */
    Class<? extends HydrationDelegate> delegate();

    /**
     * This is the name of the property we want to inject
     * @return
     */
    String propertyName();

}
