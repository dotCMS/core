package com.dotcms.content.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark classes or methods for search index operations and OpenSearch migration.
 * This annotation provides detailed information about index interaction patterns, supported
 * search engines, and migration readiness.
 *
 * Usage examples:
 * - @IndexRelation(access = IndexAccess.READ_ONLY, indexEngine = IndexEngine.ELASTICSEARCH)
 * - @IndexRelation(access = IndexAccess.READ_WRITE, indexEngine = IndexEngine.BOTH,
 *                  indexNames = {"content", "structure"})
 * - @IndexRelation(access = IndexAccess.READ_ONLY, indexEngine = IndexEngine.OPENSEARCH,
 *                  migrationReady = true, notes = "Migrated to use OpenSearch REST client")
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IndexMetadata {

    /**
     * Defines the type of index access this class or method performs.
     * @return the access type (default: READ_ONLY)
     */
    IndexAccess access() default IndexAccess.READ_ONLY;

    /**
     * Specifies which search engine(s) this class/method works with.
     * @return the supported index engine (default: ELASTICSEARCH)
     */
    IndexEngine indexEngine() default IndexEngine.ELASTICSEARCH;

    /**
     * Optional notes about index usage, migration blockers, or special considerations.
     * Useful for documenting ES-specific features or migration requirements.
     * @return descriptive notes (default: empty string)
     */
    String notes() default "";

    /**
     * Enum defining the types of index access operations.
     */
    enum IndexAccess {
        /** Only performs read operations on the search index */
        READ_ONLY,

        /** Performs both read and write operations on the search index */
        READ_WRITE,

        /** Only performs write operations on the search index */
        WRITE_ONLY
    }

    /**
     * Enum defining which search engine(s) the component works with.
     */
    enum IndexEngine {
        /** Only works with Elasticsearch */
        ELASTICSEARCH,

        /** Only works with OpenSearch */
        OPENSEARCH,

        /** Works with both Elasticsearch and OpenSearch */
        BOTH,

        /** Engine compatibility unknown or not specified */
        UNKNOWN
    }

}
