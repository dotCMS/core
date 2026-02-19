package com.dotcms.content.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark interfaces or classes as pure contracts.
 * Pure contracts should only depend on Java standard libraries and dotCMS domain classes,
 * never on external libraries like Elasticsearch, OpenSearch, or other third-party dependencies.
 *
 * This ensures:
 * - Portability across different implementations
 * - Easy testing and mocking
 * - Clear separation of concerns
 * - Future-proof abstractions
 *
 * Usage examples:
 * - @PureContract(purpose = "Content indexing abstraction")
 * - @PureContract(purpose = "Search operations contract", migrationReady = true)
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PureContract {

    /**
     * Describes the purpose or responsibility of this pure contract.
     * @return descriptive text explaining what this contract represents
     */
    String purpose() default "";

    /**
     * Optional notes about the contract design, constraints, or special considerations.
     * @return additional documentation (default: empty string)
     */
    String notes() default "";
}