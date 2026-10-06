package com.dotcms.rest.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a REST resource that must never receive cross-origin response headers.
 *
 * <p>dotCMS ships CORS switched on for every endpoint through the {@code api.cors.default.*}
 * properties, so a resource that says nothing gets {@code Access-Control-Allow-Origin: *}. That is
 * the right default for the delivery APIs a browser is meant to call. It is the wrong one for an
 * endpoint whose credential is a long-lived bearer token carrying the full authority of its owner:
 * advertising cross-origin support invites putting that token in browser JavaScript, where any
 * script on the page can read it.</p>
 *
 * <p>Annotating the resource is what makes the exemption survive: the alternative is per-resource
 * {@code api.cors.<resourcename>.*} configuration, which is keyed on the class's simple name and
 * so breaks silently the moment the class is renamed, and which cannot express "emit nothing"
 * anyway — an entry with an empty value emits an empty header rather than none.</p>
 *
 * @see com.dotcms.rest.api.CorsFilter
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoCors {
}
