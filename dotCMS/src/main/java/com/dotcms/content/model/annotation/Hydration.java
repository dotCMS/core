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

    /**
     * Through this attribute we indicate what properties of the bean annotated should be injected
     * @return
     */
    HydrateWith[] properties();

}
