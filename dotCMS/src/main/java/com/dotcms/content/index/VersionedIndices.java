package com.dotcms.content.index;

import com.twelvemonkeys.lang.StringUtil;
import java.util.Optional;

/**
 * Modern interface for storing index information with version support.
 * This interface defines methods for accessing index names for different types
 * (live, working, reindex, etc.) and supports versioning for better index management.
 *
 * @author Fabrizzio
 */
public interface VersionedIndices {

     String OPENSEARCH_3X = "3.X";

    /**
     * The live index name
     */
    Optional<String> live();

    /**
     * The working index name
     */
    Optional<String> working();

    /**
     * The reindex live index name
     */
    Optional<String> reindexLive();

    /**
     * The reindex working index name
     */
    Optional<String> reindexWorking();

    /**
     * The site search index name
     */
    Optional<String> siteSearch();

    /**
     * The version associated with these indices
     */
    String version();

    /**
     * null versioned indices are considered legacy
     */
    default boolean isLegacy(){
        final String version = version();
        return StringUtil.isEmpty(version);
    }

    /**
     * Checks if this indices info has any index names set
     */
    default boolean hasAnyIndex() {
        return live().isPresent() ||
               working().isPresent() ||
               reindexLive().isPresent() ||
               reindexWorking().isPresent() ||
               siteSearch().isPresent();
    }
}