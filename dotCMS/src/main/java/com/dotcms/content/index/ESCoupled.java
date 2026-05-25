package com.dotcms.content.index;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as containing Elasticsearch-specific code that must be reviewed
 * and decommissioned when entering Phase 3 (OS-only) of the ES→OS migration.
 *
 * <p>This annotation is informational — it does not alter runtime behaviour.
 * Use it as a searchable marker: {@code grep -r "@ESCoupled"} produces the
 * complete Phase 3 decommission backlog.</p>
 *
 * @see IndexLibraryIndependent
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ESCoupled {

    /**
     * Why this class is coupled to Elasticsearch and what action is required
     * before it can be decommissioned.
     */
    String reason();

    /**
     * Methods or inner classes that must be removed at Phase 3.
     * Leave empty when the entire class is to be decommissioned.
     */
    String[] remove() default {};
}
