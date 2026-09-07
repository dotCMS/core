package com.dotcms.content.index;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Presentation-layer visibility policy for OS-tagged ({@code .os}) indices during the
 * ES&nbsp;&rarr;&nbsp;OpenSearch migration.
 *
 * <h2>Why this lives here and not in the API</h2>
 * <p>Hiding migration indices is a <em>presentation</em> concern, not a data-access one. The
 * index-listing methods on {@link IndexAPI} / {@code ContentletIndexAPI}
 * ({@code listDotCMSIndices}, {@code getIndices}, &hellip;) are reused by operational paths
 * (optimize-all, flush-all, {@code indexExists} validation, bulk fix). Filtering inside those
 * methods would silently skip OS indices for those operations in phases&nbsp;1/2 — a behavioural
 * change disguised as a UI tweak. The complete, phase-correct set must stay intact at the API;
 * only the display sinks (the maintenance JSP and {@code IndexResourceHelper.indexStatsList})
 * apply this filter, and only those — see {@code docs/backend/OPENSEARCH_MIGRATION.md}.</p>
 *
 * <h2>Rule — phase-based, for everyone</h2>
 * <ul>
 *   <li>Phase&nbsp;3 (OS-only): OS is the live store, so {@code .os} indices are always visible.</li>
 *   <li>Phases&nbsp;0/1/2: {@code .os} indices are a migration/uniqueness artifact and are hidden
 *       from every user — regular admins never learn a migration is running.</li>
 * </ul>
 *
 * <p>The role-gated preview of {@code .os} indices was removed in issue #36360: support and QA now
 * get migration detail from the dedicated, role-gated migration-readiness endpoint
 * ({@code /api/v1/index/migration/readiness}), which is the single source of truth. This display
 * policy is therefore purely phase-based and consults no user or role.</p>
 *
 * <p>OS-origin detection always goes through {@link IndexTag#isTagged(String)}, never
 * {@code name.endsWith(".os")}, per the {@link IndexTag} contract.</p>
 */
public final class MigrationIndexVisibility {

    /**
     * Config key holding the {@link com.dotmarketing.business.Role#getRoleKey() role key} whose
     * members may read the role-gated <em>migration-readiness endpoint</em>
     * ({@code /api/v1/index/migration/readiness}). Defaults to {@value #DEFAULT_VISIBILITY_ROLE_KEY}.
     * It no longer governs the index portlet display (which is purely phase-based since issue #36360).
     */
    public static final String VISIBILITY_ROLE_KEY = "OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY";

    /** Default role key allowed to read the migration-readiness endpoint. */
    public static final String DEFAULT_VISIBILITY_ROLE_KEY = "os_migration_qa";

    private MigrationIndexVisibility() {
        throw new AssertionError("Utility class — do not instantiate");
    }

    /**
     * Whether OS-tagged ({@code .os}) indices are shown in the current phase — only in Phase&nbsp;3,
     * where OpenSearch is the live store. Before Phase&nbsp;3 they are a migration artifact and stay
     * hidden from everyone.
     */
    public static boolean showMigrationIndices() {
        return MigrationPhase.current().isMigrationComplete();
    }

    /**
     * Returns {@code indexNames} with OS-tagged ({@code .os}) entries removed outside Phase&nbsp;3;
     * in Phase&nbsp;3 (or for a null/empty input) the list is returned unchanged.
     *
     * @param indexNames the full, phase-correct list of index names; {@code null}/empty is returned
     *                   as-is
     * @return a filtered copy, or the original list when no filtering applies
     */
    public static List<String> filter(final List<String> indexNames) {
        if (indexNames == null || indexNames.isEmpty() || showMigrationIndices()) {
            return indexNames;
        }
        return indexNames.stream()
                .filter(name -> !IndexTag.OS.isTagged(name))
                .collect(Collectors.toList());
    }
}
