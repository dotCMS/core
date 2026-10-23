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
 * Resolves the display data of the failed reindex records — inode, language, title, content type
 * and whether the content is archived — without loading any contentlet.
 *
 * <p>The failed records are often the oversized contentlets #37269 withholds. Loading each one
 * to read a few values put tens of megabytes per record on the heap and left them in the
 * contentlet cache (review of #37924). This reads only the needed columns, never
 * {@code contentlet_as_json}, in one query per batch. Both lookups use primary keys:
 * {@code contentlet_version_info} by its leading {@code identifier} column and
 * {@code contentlet} by {@code inode}.</p>
 */
final class DisplayVersionFinder {

    /** Keys per query; keeps the {@code IN} list well inside the planner's index path. */
    private static final int DEFAULT_BATCH_SIZE = 500;

    /**
     * The working version of one language per identifier, default variant: a non-archived
     * language first, then the lowest language id, so the choice is stable.
     */
    private static final String BY_IDENTIFIER = "SELECT DISTINCT ON (cvi.identifier)"
            + " cvi.identifier AS lookup_key, c.inode, c.language_id, c.title, c.structure_inode,"
            + " cvi.deleted"
            + " FROM contentlet_version_info cvi"
            + " JOIN contentlet c ON c.inode = cvi.working_inode"
            + " WHERE cvi.variant_id = 'DEFAULT' AND cvi.identifier IN (%s)"
            + " ORDER BY cvi.identifier, cvi.deleted, cvi.lang";

    /** Exact versions, with the archived flag of their language. */
    private static final String BY_INODE = "SELECT c.inode AS lookup_key, c.inode, c.language_id,"
            + " c.title, c.structure_inode, COALESCE(cvi.deleted, false) AS deleted"
            + " FROM contentlet c"
            + " LEFT JOIN contentlet_version_info cvi ON cvi.identifier = c.identifier"
            + " AND cvi.lang = c.language_id AND cvi.variant_id = c.variant_id"
            + " WHERE c.inode IN (%s)";

    private final int batchSize;

    /** Queries in batches of {@value #DEFAULT_BATCH_SIZE} keys. */
    DisplayVersionFinder() {
        this(DEFAULT_BATCH_SIZE);
    }

    /**
     * Queries in batches of the given size. Lets tests cross a batch boundary with few rows.
     *
     * @param batchSize keys per query, at least 1
     */
    DisplayVersionFinder(final int batchSize) {
        this.batchSize = Math.max(1, batchSize);
    }

    /**
     * Resolves a version to show for each identifier. The version rule is the one of
     * {@code findContentletByIdentifierAnyLanguage} — the working version of a default-variant
     * language — except that archived content is still resolved, flagged as such.
     *
     * @param identifiers the identifiers of the failed records
     * @return the display data by identifier; an identifier with no version is absent
     * @throws DotDataException if the query fails
     */
    Map<String, DisplayVersion> find(final Collection<String> identifiers)
            throws DotDataException {
        return query(BY_IDENTIFIER, identifiers);
    }

    /**
     * Resolves the given exact versions, such as the ones named by withheld documents.
     *
     * @param inodes the version inodes
     * @return the display data by inode; an inode with no version is absent
     * @throws DotDataException if the query fails
     */
    Map<String, DisplayVersion> findByInodes(final Collection<String> inodes)
            throws DotDataException {
        return query(BY_INODE, inodes);
    }

    private Map<String, DisplayVersion> query(final String sqlTemplate,
            final Collection<String> keys) throws DotDataException {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        final List<String> distinct = keys.stream().distinct().collect(Collectors.toList());
        final Map<String, String> typeVariables = new HashMap<>();
        final Map<String, DisplayVersion> found = new HashMap<>();
        for (int from = 0; from < distinct.size(); from += batchSize) {
            final List<String> batch = distinct.subList(from,
                    Math.min(from + batchSize, distinct.size()));
            final DotConnect dc = new DotConnect().setSQL(String.format(sqlTemplate,
                    String.join(",", Collections.nCopies(batch.size(), "?"))));
            batch.forEach(dc::addParam);
            for (final Map<String, Object> row : dc.loadObjectResults()) {
                final String title = (String) row.get("title");
                final String structureInode = (String) row.get("structure_inode");
                found.put((String) row.get("lookup_key"), new DisplayVersion(
                        (String) row.get("inode"),
                        ((Number) row.get("language_id")).longValue(),
                        UtilMethods.isSet(title) ? title : null,
                        typeVariables.computeIfAbsent(structureInode,
                                DisplayVersionFinder::typeVariable),
                        Boolean.TRUE.equals(row.get("deleted"))));
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
