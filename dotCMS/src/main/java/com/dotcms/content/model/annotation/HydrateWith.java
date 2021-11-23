package com.dotcms.content.model.annotation;

import com.dotcms.content.model.hydration.HydrationDelegate;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface HydrateWith {

    Class<? extends HydrationDelegate> delegate();

    String propertyName();

}
