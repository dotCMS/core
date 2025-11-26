package com.dotcms.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used in Immutables to allow null values in the generated classes collections
 * See <a href="https://immutables.github.io/immutable.html#nulls-in-collection">https://immutables.github.io/immutable.html#nulls-in-collection</a>
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {

}