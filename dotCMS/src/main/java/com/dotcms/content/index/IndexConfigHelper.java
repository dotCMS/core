package com.dotcms.content.index;

import com.dotcms.content.index.opensearch.OSIndexProperty;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotmarketing.util.Config;

/**
 * Central helper for reading index-layer configuration properties.
 *
 * <h2>Migration phases</h2>
 * <p>The active phase is read from the feature flag
 * {@code FEATURE_FLAG_OPEN_SEARCH_PHASE} (an ordinal string {@code "0"}–{@code "3"}).
 * Use {@link MigrationPhase#current()} to obtain it, or the convenience
 * predicates {@link #isOpenSearchWriteEnabled()} / {@link #isOpenSearchReadEnabled()}.</p>
 *
 * <ul>
 *   <li><strong>PHASE_0_MIGRATION_NOT_STARTED</strong> — ES only (default, no OS traffic).</li>
 *   <li><strong>PHASE_1_DUAL_WRITE_ES_READS</strong> — writes to both OS and ES; reads from ES.</li>
 *   <li><strong>PHASE_2_DUAL_WRITE_OS_READS</strong> — writes to both OS and ES; reads from OS.</li>
 *   <li><strong>PHASE_3_OPENSEARCH_ONLY</strong> — OS only; ES is decommissioned.</li>
 * </ul>
 *
 * <h2>OS → ES fallback resolution</h2>
 * <p>The typed {@code getString}, {@code getInt}, and {@code getBoolean} methods
 * follow a three-step resolution for every {@link OSIndexProperty}:</p>
 * <ol>
 *   <li>Read the OpenSearch key ({@link OSIndexProperty#osKey}).</li>
 *   <li>If absent <em>and</em> {@link OSIndexProperty#esFallback} is non-null,
 *       read the Elasticsearch fallback key.</li>
 *   <li>If still absent, return the caller-supplied {@code defaultValue}.</li>
 * </ol>
 * <p>Existence is determined by checking whether the raw string stored under the
 * key is non-null, which avoids treating {@code "0"} or {@code "false"} as
 * "not set" for numeric and boolean properties.</p>
 */
public interface IndexConfigHelper {

    // -------------------------------------------------------------------------
    // Migration phase
    // -------------------------------------------------------------------------

    /**
     * Describes the four stages of the ES → OpenSearch migration.
     *
     * <p>The active phase is resolved at call-time from the feature flag
     * {@code FEATURE_FLAG_OPEN_SEARCH_PHASE}. Its value must be the ordinal
     * integer of the desired phase ({@code "0"} through {@code "3"}).
     * Unrecognised values fall back to {@link #PHASE_0_MIGRATION_NOT_STARTED}.</p>
     *
     * <pre>
     * Phase 0 — ES only (default)
     *   writes → ES only
     *   reads  → ES only
     *
     * Phase 1 — dual-write
     *   writes → OS + ES  (ES result authoritative)
     *   reads  → ES only
     *
     * Phase 2 — dual-write + OS reads
     *   writes → OS + ES  (ES result authoritative)
     *   reads  → OS
     *
     * Phase 3 — OS only
     *   writes → OS only
     *   reads  → OS only
     * </pre>
     */
    enum MigrationPhase {
        /** Phase 0: ES only — migration has not started, OS receives no traffic. */
        PHASE_0_MIGRATION_NOT_STARTED,
        /** Phase 1: dual-write — both indices receive writes; reads still served by ES. */
        PHASE_1_DUAL_WRITE_ES_READS,
        /** Phase 2: dual-write + OS reads — both indices receive writes; reads now served by OS. */
        PHASE_2_DUAL_WRITE_OS_READS,
        /** Phase 3: OS only — ES is decommissioned, all traffic goes to OS. */
        PHASE_3_OPENSEARCH_ONLY;

        /** Feature-flag key that stores the active phase ordinal. */
        public static final String FLAG_KEY = FeatureFlagName.FEATURE_FLAG_OPEN_SEARCH_PHASE;

        /**
         * Returns the currently configured {@link MigrationPhase}.
         *
         * <p>Reads {@code FEATURE_FLAG_OPEN_SEARCH_PHASE} from dotCMS config.
         * If the value is absent or unrecognized, {@link #PHASE_0_MIGRATION_NOT_STARTED}
         * is returned.</p>
         */
        public static MigrationPhase current() {
            final int ordinal = Config.getIntProperty(FLAG_KEY, 0);
            final MigrationPhase[] values = values();
            if (ordinal >= 0 && ordinal < values.length) {
                return values[ordinal];
            }
            return PHASE_0_MIGRATION_NOT_STARTED;
        }

        // ── Semantic predicates ──────────────────────────────────────────────

        /** {@code true} in phase 0: no OS traffic at all. */
        public boolean isMigrationNotStarted() {
            return this == PHASE_0_MIGRATION_NOT_STARTED;
        }

        /** {@code true} in phases 1 and 2: writes fan out to both OS and ES. */
        public boolean isDualWrite() {
            return this == PHASE_1_DUAL_WRITE_ES_READS || this == PHASE_2_DUAL_WRITE_OS_READS;
        }

        /** {@code true} in phase 3: only OS receives traffic, ES is fully decommissioned. */
        public boolean isMigrationComplete() {
            return this == PHASE_3_OPENSEARCH_ONLY;
        }

        // ── Routing helpers (used by IndexConfigHelper predicates) ───────────

        /** {@code true} when OS should serve read operations (phases 2, 3). */
        public boolean isReadEnabled() {
            return this == PHASE_2_DUAL_WRITE_OS_READS || this == PHASE_3_OPENSEARCH_ONLY;
        }
    }

    static boolean isMigrationNotStarted(){
        return MigrationPhase.current().isMigrationNotStarted();
    }

    static boolean isMigrationStarted(){
        return !isMigrationNotStarted() || isDualWrite() || isReadEnabled();
    }

    static boolean isDualWrite(){
        return MigrationPhase.current().isDualWrite();
    }

    static boolean isMigrationComplete(){
        return MigrationPhase.current().isMigrationComplete();
    }

    static boolean isReadEnabled(){
        return MigrationPhase.current().isReadEnabled();
    }

    // -------------------------------------------------------------------------
    // Typed property resolution with OS → ES fallback
    // -------------------------------------------------------------------------

    /**
     * Resolves a {@link String} property following the OS → ES fallback chain.
     *
     * @param prop         the property descriptor
     * @param defaultValue value returned when neither key is set
     * @return the resolved value, or {@code defaultValue}
     */
    static String getString(final OSIndexProperty prop, final String defaultValue) {
        final String osValue = Config.getStringProperty(prop.osKey, null);
        if (osValue != null) {
            return osValue;
        }
        if (prop.esFallback != null) {
            final String esValue = Config.getStringProperty(prop.esFallback, null);
            if (esValue != null) {
                return esValue;
            }
        }
        return defaultValue;
    }

    /**
     * Resolves an {@code int} property following the OS → ES fallback chain.
     *
     * <p>Existence is checked via the raw string value so that an explicitly
     * configured {@code 0} is not mistaken for "not set".</p>
     *
     * @param prop         the property descriptor
     * @param defaultValue value returned when neither key is set
     * @return the resolved value, or {@code defaultValue}
     */
    static int getInt(final OSIndexProperty prop, final int defaultValue) {
        if (Config.getStringProperty(prop.osKey, null) != null) {
            return Config.getIntProperty(prop.osKey, defaultValue);
        }
        if (prop.esFallback != null && Config.getStringProperty(prop.esFallback, null) != null) {
            return Config.getIntProperty(prop.esFallback, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Resolves a {@code boolean} property following the OS → ES fallback chain.
     *
     * <p>Existence is checked via the raw string value so that an explicitly
     * configured {@code false} is not mistaken for "not set".</p>
     *
     * @param prop         the property descriptor
     * @param defaultValue value returned when neither key is set
     * @return the resolved value, or {@code defaultValue}
     */
    static boolean getBoolean(final OSIndexProperty prop, final boolean defaultValue) {
        if (Config.getStringProperty(prop.osKey, null) != null) {
            return Config.getBooleanProperty(prop.osKey, defaultValue);
        }
        if (prop.esFallback != null && Config.getStringProperty(prop.esFallback, null) != null) {
            return Config.getBooleanProperty(prop.esFallback, defaultValue);
        }
        return defaultValue;
    }
}
