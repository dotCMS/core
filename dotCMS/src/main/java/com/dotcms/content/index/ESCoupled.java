package com.dotcms.content.index;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type or method as containing Elasticsearch-specific code that must be reviewed
 * and decommissioned before or upon entering Phase 3 (OS-only) of the ES→OS migration.
 *
 * <p>This is the semantic counterpart of {@link IndexLibraryIndependent}. A class annotated
 * with {@code @ESCoupled} either:
 * <ul>
 *   <li>Implements ES-specific behaviour with no OS counterpart yet created, or</li>
 *   <li>Exposes ES vendor types in its public API that must be replaced with
 *       vendor-neutral equivalents before ES can be shut down.</li>
 * </ul>
 *
 * <p>This annotation is informational — it does not alter runtime behaviour.
 * It serves as a searchable marker during Phase 3 planning and cleanup.
 *
 * @see IndexLibraryIndependent
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ESCoupled {

    /**
     * Human-readable explanation of what ES-specific code exists and what
     * action is required before decommission.
     */
    String reason();

    /**
     * GitHub issue tracking the decommission or migration work.
     * Format: "#12345"
     */
    String trackedIn() default "";

    /**
     * Migration phase at which this coupling must be resolved.
     * Defaults to 3 (OS-only). Set to 2 if action is required before OS reads activate.
     */
    int phase() default 3;
}
