package com.dotcms.content.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or method as a participant in search-index operations during the ES → OS migration.
 *
 * <p>The {@link #access()} array declares which kinds of operations the annotated element
 * performs. Use a single value for read-only or write-only classes, and both values for
 * router/facade classes that handle both paths:</p>
 *
 * <pre>
 * // Read-only consumer (e.g. search query factory)
 * {@literal @}IndexRouter(access = IndexAccess.READ)
 *
 * // Write-only producer (e.g. bulk-index pipeline)
 * {@literal @}IndexRouter(access = IndexAccess.WRITE)
 *
 * // Router/facade — handles reads and writes (e.g. IndexAPIImpl, ContentletIndexAPIImpl)
 * {@literal @}IndexRouter(access = {IndexAccess.READ, IndexAccess.WRITE})
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IndexRouter {

    /**
     * The kinds of index operations performed by the annotated element.
     *
     * <p>Use {@code {IndexAccess.READ, IndexAccess.WRITE}} for router/facade classes
     * that delegate to both ES and OS providers. Use a single value for classes that
     * only read or only write.</p>
     *
     * @return one or more access kinds (default: {@code READ})
     */
    IndexAccess[] access() default IndexAccess.READ;

    /**
     * Optional notes about index usage, migration blockers, or special considerations.
     *
     * @return descriptive notes (default: empty string)
     */
    String notes() default "";

    /**
     * The kind of index access an annotated element performs.
     */
    enum IndexAccess {
        /** The element reads from the search index (queries, lookups, health checks). */
        READ,

        /** The element writes to the search index (index, delete, bulk, mapping). */
        WRITE
    }

}