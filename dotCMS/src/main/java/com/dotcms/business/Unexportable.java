package com.dotcms.business;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A {@link com.dotcms.contenttype.model.field.Field} annotated with the <code>Unexportable</code> annotation will
 * not be included in the resulting CSV when exporting content that includes it
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Unexportable {
}