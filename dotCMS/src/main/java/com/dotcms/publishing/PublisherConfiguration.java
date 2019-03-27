package com.dotcms.publishing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation class allows to set the "isStatic" configuration value
 * for the Push Publishers classes where the the push should be static
 * @author Oswaldo Gallango
 *
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublisherConfiguration {

	/**
	 * Return true if the publisher is a static publisher
	 * @return true if the publisher is a static publisher, false if not
	 */
	boolean isStatic() default false;
}
