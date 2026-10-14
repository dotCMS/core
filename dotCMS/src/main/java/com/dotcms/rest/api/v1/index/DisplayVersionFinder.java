package com.dotcms.rest.api.v1.index;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Resolves the display data of the failed reindex records — inode, language, title and content
 * type — without loading any contentlet.
 *
 * <p>The failed records are often the oversized contentlets #37269 withholds. Loading each one
 * to read three values put tens of megabytes per record on the heap and left them in the
 * contentlet cache (review of #37924). This reads only the needed columns, never
 * {@code contentlet_as_json}, in one query per batch of identifiers. Both lookups use primary
 * keys: {@code contentlet_version_info} by its leading {@code identifier} column and
 * {@code contentlet} by {@code inode}.</p>
 *
 * <p>The version rule is the one of {@code findContentletByIdentifierAnyLanguage}: the working
 * version of a non-archived, default-variant language of the identifier. When several languages
 * qualify, the lowest language id is used so the result is stable.</p>
 */
final class DisplayVersionFinder {

    /** Identifiers per query; keeps the {@code IN} list well inside the planner's index path. */
    private static final int DEFAULT_BATCH_SIZE = 500;

    private static final String QUERY_PREFIX = "SELECT DISTINCT ON (cvi.identifier)"
            + " cvi.identifier, c.inode, c.language_id, c.title, c.structure_inode"
            + " FROM contentlet_version_info cvi"
            + " JOIN contentlet c ON c.inode = cvi.working_inode"
            + " WHERE cvi.deleted = false AND cvi.variant_id = 'DEFAULT'"
            + " AND cvi.identifier IN (";

    private static final String QUERY_SUFFIX = ") ORDER BY cvi.identifier, cvi.lang";

    private final int batchSize;

    /** Queries in batches of {@value #DEFAULT_BATCH_SIZE} identifiers. */
    DisplayVersionFinder() {
        this(DEFAULT_BATCH_SIZE);
    }

    /**
     * Queries in batches of the given size. Lets tests cross a batch boundary with few rows.
     *
     * @param batchSize identifiers per query, at least 1
     */
    DisplayVersionFinder(final int batchSize) {
        this.batchSize = Math.max(1, batchSize);
    }

    /**
     * Resolves the display data of the given identifiers.
     *
     * @param identifiers the identifiers of the failed records
     * @return the display data by identifier; an identifier with no qualifying version (archived
     *         or no longer existing) is absent
     * @throws DotDataException if the query fails
     */
    Map<String, DisplayVersion> find(final Collection<String> identifiers)
            throws DotDataException {
        if (identifiers == null || identifiers.isEmpty()) {
            return Collections.emptyMap();
        }
        final List<String> distinct = identifiers.stream().distinct()
                .collect(Collectors.toList());
        final Map<String, String> typeVariables = new HashMap<>();
        final Map<String, DisplayVersion> found = new HashMap<>();
        for (int from = 0; from < distinct.size(); from += batchSize) {
            final List<String> batch = distinct.subList(from,
                    Math.min(from + batchSize, distinct.size()));
            final DotConnect dc = new DotConnect().setSQL(QUERY_PREFIX
                    + String.join(",", Collections.nCopies(batch.size(), "?")) + QUERY_SUFFIX);
            batch.forEach(dc::addParam);
            for (final Map<String, Object> row : dc.loadObjectResults()) {
                final String title = (String) row.get("title");
                final String structureInode = (String) row.get("structure_inode");
                found.put((String) row.get("identifier"), new DisplayVersion(
                        (String) row.get("inode"),
                        ((Number) row.get("language_id")).longValue(),
                        UtilMethods.isSet(title) ? title : null,
                        typeVariables.computeIfAbsent(structureInode,
                                DisplayVersionFinder::typeVariable)));
            }
        }
        return found;
    }

    /**
     * Resolves a content type variable through the content type API, which serves it from cache.
     *
     * @return the variable, or {@code null} when the type cannot be resolved
     */
    private static String typeVariable(final String structureInode) {
        if (structureInode == null) {
            return null;
        }
        return Try.of(() -> APILocator.getContentTypeAPI(APILocator.systemUser())
                        .find(structureInode).variable())
                .onFailure(e -> Logger.debug(DisplayVersionFinder.class,
                        "Unable to resolve content type " + structureInode + ": " + e.getMessage()))
                .getOrNull();
    }
}
