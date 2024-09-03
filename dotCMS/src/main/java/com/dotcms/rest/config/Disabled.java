package com.dotcms.rest.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Annotation to mark a resource as disabled.
 * This is useful now that resources are activated by classpath scanning.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Disabled {
    Response.Status status() default Status.NOT_FOUND;
    String message() default "";
}
