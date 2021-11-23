package com.dotcms.content.model.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotations serves as te entry point to feed classes with metadata to inject properties with data generated on the fly (hydrate)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Hydration {

    HydrateWith[] properties();

}
