package com.dotcms.shutdown;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the execution order of a {@link ShutdownTask}.
 * Lower numbers execute first.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ShutdownOrder {
    int value();
}
