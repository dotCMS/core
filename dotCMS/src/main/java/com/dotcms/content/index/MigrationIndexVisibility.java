package com.dotcms.content.index;

import com.dotcms.content.index.IndexConfigHelper.MigrationPhase;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
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
 * only the two display sinks (the maintenance JSP and {@code IndexResourceHelper.indexStatsList})
 * apply this filter, and only those — see {@code docs/backend/OPENSEARCH_MIGRATION.md}.</p>
 *
 * <h2>Rule</h2>
 * <ul>
 *   <li>Phase&nbsp;3 (OS-only): OS is the live store, so {@code .os} indices are always visible.</li>
 *   <li>Phases&nbsp;0/1/2: {@code .os} indices are a migration/uniqueness artifact and are hidden,
 *       <em>unless</em> the acting user holds the configured QA/preview role
 *       ({@value #VISIBILITY_ROLE_KEY}, default {@value #DEFAULT_VISIBILITY_ROLE_KEY}).</li>
 * </ul>
 *
 * <p>The acting {@link User} is supplied explicitly by each display sink (both are authenticated
 * admin requests where the user is always available) — never resolved from a thread-local inside
 * this policy, so it is safe to unit-test and free of request-context coupling.</p>
 *
 * <p>OS-origin detection always goes through {@link IndexTag#isTagged(String)}, never
 * {@code name.endsWith(".os")}, per the {@link IndexTag} contract.</p>
 */
public final class MigrationIndexVisibility {

    /**
     * Config key holding the {@link Role#getRoleKey() role key} whose members may preview
     * OS-tagged ({@code .os}) indices before Phase&nbsp;3. Defaults to
     * {@value #DEFAULT_VISIBILITY_ROLE_KEY}.
     */
    public static final String VISIBILITY_ROLE_KEY = "OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY";

    /** Default role key allowed to preview migration ({@code .os}) indices. */
    public static final String DEFAULT_VISIBILITY_ROLE_KEY = "os_migration_qa";

    private MigrationIndexVisibility() {
        throw new AssertionError("Utility class — do not instantiate");
    }

    /**
     * Whether {@code user} may see OS-tagged ({@code .os}) indices in the current phase.
     *
     * @param user the acting user; {@code null} is treated as "not allowed" outside Phase&nbsp;3
     * @return {@code true} in Phase&nbsp;3, or when {@code user} holds the configured QA role
     */
    public static boolean canSeeMigrationIndices(final User user) {
        if (MigrationPhase.current().isMigrationComplete()) {
            return true;
        }
        if (user == null) {
            return false;
        }
        final String roleKey = Config.getStringProperty(VISIBILITY_ROLE_KEY,
                DEFAULT_VISIBILITY_ROLE_KEY);
        if (!UtilMethods.isSet(roleKey)) {
            return false;
        }
        return Try.of(() -> {
            final Role role = APILocator.getRoleAPI().loadRoleByKey(roleKey);
            return role != null && APILocator.getRoleAPI().doesUserHaveRole(user, role);
        }).getOrElse(false);
    }

    /**
     * Returns {@code indexNames} with OS-tagged ({@code .os}) entries removed when {@code user}
     * is not allowed to see them; otherwise returns the list unchanged.
     *
     * @param indexNames the full, phase-correct list of index names; {@code null}/empty is
     *                   returned as-is
     * @param user       the acting user
     * @return a filtered copy, or the original list when no filtering applies
     */
    public static List<String> filter(final List<String> indexNames, final User user) {
        if (indexNames == null || indexNames.isEmpty() || canSeeMigrationIndices(user)) {
            return indexNames;
        }
        return indexNames.stream()
                .filter(name -> !IndexTag.OS.isTagged(name))
                .collect(Collectors.toList());
    }
}