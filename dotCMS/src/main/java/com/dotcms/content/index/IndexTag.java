package com.dotcms.content.index;

import java.util.Optional;

/**
 * Vendor-origin tag for raw index-name strings.
 *
 * <h2>Purpose</h2>
 * <p>When flat {@code String} index names from the OpenSearch backend
 * ({@code VersionedIndicesAPI}) and the legacy Elasticsearch backend
 * ({@code IndiciesInfo}) are mixed in the same collection there is no
 * structural way to tell them apart. {@code IndexTag} solves this by
 * applying a stable marker and providing matching helpers to read it back.</p>
 *
 * <h2>Markers</h2>
 * <ul>
 *   <li>{@link #OS} — suffix {@code ".os"}, e.g. {@code cluster_08abc3.live_20240315.os}.
 *       The tag is part of the canonical OS name at every layer: service/API logical names
 *       (e.g. {@code working_20260406.os}), the {@code indicies} DB row, and the physical
 *       index in the cluster itself. It is NOT a DB-only artifact.</li>
 *   <li>{@link #ES} — no marker (empty prefix and suffix); used only as a routing constant
 *       so that untagged legacy Elasticsearch names resolve to the ES provider</li>
 * </ul>
 *
 * <h2>Why an explicit marker</h2>
 * <p>The tag solves three concurrent problems while ES and OS coexist:</p>
 * <ol>
 *   <li><b>DB PK uniqueness</b> in the shared {@code indicies} table — without an explicit
 *       marker, the legacy ES row and the new OS row for the same logical name collide.</li>
 *   <li><b>Cluster name distinction</b> in single-cluster test profiles where
 *       {@code DOT_ES_ENDPOINTS == OS_ENDPOINTS} (e.g. the {@code opensearch-upgrade} Maven
 *       profile) — without the tag, a Phase 1 fan-out hits {@code resource_already_exists}
 *       on the second write.</li>
 *   <li><b>Tag-dispatch routing</b> — a tagged name in a caller's hand carries semantic intent
 *       ("this belongs to the new index set"), letting the router skip phase logic and go
 *       straight to the OS provider.</li>
 * </ol>
 * <p>The literal {@code .os} is not vendor-specific — it is simply the chosen distinction
 * marker. Changing it is a one-line edit in this enum; no caller hardcodes the suffix.</p>
 *
 * <h2>Sole responsibility</h2>
 * <p>This enum is the ONLY place in the codebase that may manipulate the marker. Helpers
 * elsewhere — including in providers, routers, mapping helpers, factories, tests, and debug
 * utilities — MUST delegate to {@link #tag}, {@link #untag}, {@link #strip},
 * {@link #isTagged}, {@link #resolve}, or {@link #vendorOf}. Any direct string operation on
 * the marker outside this class (e.g. {@code name + ".os"}, {@code name.endsWith(".os")},
 * {@code name.substring(...)} to remove it) is a bug, even if it appears to work today: a
 * future change to the literal or to the prefix/suffix position will silently bypass it.</p>
 *
 * <h2>Default vendor</h2>
 * <p>{@link #resolve(String)} returns the vendor whose marker the name
 * carries, or {@link #ES} when no marker is found. This makes untagged
 * (legacy) index names transparently route to Elasticsearch.</p>
 *
 * <h2>Tag-dispatch routing pattern</h2>
 * <pre>{@code
 * // Route a tagged index name to the correct provider.
 * // Do NOT strip the tag before calling the provider — the provider's toPhysicalName is
 * // idempotent and accepts both tagged and untagged forms. The tag is part of the canonical
 * // name end-to-end.
 * IndexTag vendor = IndexTag.resolve(name);  // never null
 * ContentletIndexOperations target = (vendor == IndexTag.OS) ? osImpl : esImpl;
 * target.someOperation(target.toPhysicalName(name));
 * }</pre>
 *
 * <p>Use {@link #vendorOf(String)} instead of {@link #resolve(String)} when
 * you need to distinguish "explicitly tagged as ES" from "no tag present".</p>
 *
 * @see PhaseRouter
 */
public enum IndexTag {

    /**
     * Marks an index name as originating from the OpenSearch backend.
     * Storage form: {@code <logical-name>.os} (suffix-based, DB uniqueness artifact).
     */
    OS("", ".os"),

    /**
     * Routing constant for the legacy Elasticsearch backend.
     * No marker is applied: both prefix and suffix are empty, so {@link #tag} is a no-op
     * and {@link #isTagged} always returns {@code false}. Untagged names resolve to ES
     * via {@link #resolve(String)} defaulting to this constant.
     */
    ES("", "");

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** Prefix prepended to tagged names. Empty for suffix-based tags. */
    public final String prefix;

    /** Suffix appended to tagged names. Empty for prefix-based tags. */
    public final String suffix;

    IndexTag(final String prefix, final String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    // -------------------------------------------------------------------------
    // Instance operations
    // -------------------------------------------------------------------------

    /**
     * Applies this vendor's marker to {@code indexName}.
     *
     * <p>Idempotent: if {@code indexName} is already tagged with <em>any</em>
     * vendor marker it is returned unchanged, preventing double-tagging.</p>
     *
     * @param indexName raw or already-tagged index name; {@code null} returns {@code null}
     * @return tagged name, e.g. {@code "cluster_08abc3.working_20260406.os"}
     */
    public String tag(final String indexName) {
        if (indexName == null) {
            return null;
        }
        // Already tagged — do not stack markers
        for (final IndexTag existing : values()) {
            if (existing.isTagged(indexName)) {
                return indexName;
            }
        }
        return prefix + indexName + suffix;
    }

    /**
     * Removes this vendor's marker from {@code taggedName}.
     *
     * <p>If the name does not carry this marker it is returned unchanged —
     * no exception is thrown. Use {@link #isTagged(String)} to guard first
     * when strict matching is required.</p>
     *
     * @param taggedName tagged or untagged name; {@code null} returns {@code null}
     * @return the name without the vendor marker
     */
    public String untag(final String taggedName) {
        if (taggedName == null) {
            return null;
        }
        if (!prefix.isEmpty() && taggedName.startsWith(prefix)) {
            return taggedName.substring(prefix.length());
        }
        if (!suffix.isEmpty() && taggedName.endsWith(suffix)) {
            return taggedName.substring(0, taggedName.length() - suffix.length());
        }
        return taggedName;
    }

    /**
     * Returns {@code true} when {@code name} carries this vendor's marker.
     *
     * @param name any string; {@code null} returns {@code false}
     */
    public boolean isTagged(final String name) {
        if (name == null) {
            return false;
        }
        return (!prefix.isEmpty() && name.startsWith(prefix))
                || (!suffix.isEmpty() && name.endsWith(suffix));
    }

    // -------------------------------------------------------------------------
    // Static helpers
    // -------------------------------------------------------------------------

    /**
     * Identifies which vendor tagged {@code name}, or empty if unrecognised.
     *
     * <p>Use this when you need to distinguish "explicitly tagged as ES"
     * from "no tag present". For simple routing use {@link #resolve(String)}.</p>
     *
     * @param name tagged or raw index name; {@code null} returns empty
     * @return the matching {@link IndexTag}, or {@link Optional#empty()}
     */
    public static Optional<IndexTag> vendorOf(final String name) {
        if (name == null) {
            return Optional.empty();
        }
        for (final IndexTag tag : values()) {
            if (tag.isTagged(name)) {
                return Optional.of(tag);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves the vendor tag for {@code name}, defaulting to {@link #ES} when
     * the name carries no recognised marker.
     *
     * <p>Use this in routing code where an untagged name must be treated as a
     * legacy Elasticsearch index:</p>
     * <pre>{@code
     * IndexTag vendor = IndexTag.resolve(name); // never null
     * String raw = vendor.untag(name);
     * }</pre>
     *
     * @param name any index name, tagged or untagged; {@code null} returns {@link #ES}
     * @return the vendor whose marker the name carries, or {@link #ES} if none
     */
    public static IndexTag resolve(final String name) {
        return vendorOf(name).orElse(ES);
    }

    /**
     * Strips any known vendor marker from {@code name}.
     *
     * <p>If no marker matches the name is returned unchanged, making this safe
     * to call on already-untagged strings.</p>
     *
     * @param name tagged or raw index name; {@code null} returns {@code null}
     * @return the raw index name without any vendor marker
     */
    public static String strip(final String name) {
        if (name == null) {
            return null;
        }
        for (final IndexTag tag : values()) {
            if (tag.isTagged(name)) {
                return tag.untag(name);
            }
        }
        return name;
    }
}
