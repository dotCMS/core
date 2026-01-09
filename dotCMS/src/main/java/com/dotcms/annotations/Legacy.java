package com.dotcms.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the marked element is part of a legacy implementation.
 * This annotation can be used to signify that the annotated class, field, method, parameter,
 * constructor, local variable, annotation type, package, type parameter, type use, or module
 * represents a legacy construct.
 *
 * It serves as a way to document and identify elements that were created for legacy purposes,
 * and can help developers in understanding or managing old code.
 *
 * The annotation applies to a wide range of element types as denoted by the supported
 * ElementType values.
 *
 * The retention policy of this annotation is runtime, meaning it is preserved by the JVM so it
 * can be read reflectively at runtime.
 */
@Target({
        ElementType.TYPE,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.CONSTRUCTOR,
        ElementType.LOCAL_VARIABLE,
        ElementType.ANNOTATION_TYPE,
        ElementType.PACKAGE,
        ElementType.TYPE_PARAMETER,
        ElementType.TYPE_USE,
        ElementType.MODULE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface Legacy {

}
