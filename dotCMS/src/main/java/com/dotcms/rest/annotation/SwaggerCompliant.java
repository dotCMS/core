package com.dotcms.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark REST resource classes that have been updated to comply with
 * the dotCMS REST API Development Guide standards for Swagger/OpenAPI annotations.
 * 
 * This annotation serves as a marker for progressive testing during the migration
 * to full annotation compliance. Test classes can filter by this annotation to
 * only validate resource classes that have been properly updated.
 * 
 * Once all resource classes are updated and annotated, this annotation can be
 * removed and tests can validate all resource classes automatically.
 * 
 * @see com.dotcms.rest.RestEndpointAnnotationValidationTest
 * @see com.dotcms.rest.RestEndpointAnnotationComplianceTest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SwaggerCompliant {
    
    /**
     * Optional description of what was fixed or updated in this resource class.
     * This can be useful for tracking the progress of the migration.
     */
    String value() default "";
    
    /**
     * Version of the compliance standards this resource class follows.
     * Defaults to "1.0" for the initial implementation.
     */
    String version() default "1.0";
    
    /**
     * Batch number for progressive rollout. Used to organize the migration
     * into manageable chunks for PR management and testing.
     * Defaults to 1 for the initial batch.
     */
    int batch() default 1;
}