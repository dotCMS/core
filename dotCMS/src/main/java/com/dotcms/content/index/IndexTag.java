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
 * prepending a short, stable prefix and providing matching helpers to
 * read it back.</p>
 *
 * <h2>Prefixes</h2>
 * <ul>
 *   <li>{@link #OS} — prefix {@code "os::"}, e.g. {@code os::contentlet_live_20240315}</li>
 *   <li>{@link #ES} — prefix {@code "es::"}, e.g. {@code es::contentlet_live_20240315}</li>
 * </ul>
 * <p>The {@code ::} separator is used because colons never appear in valid
 * Elasticsearch / OpenSearch index names, making the prefix unambiguous.</p>
 *
 * <h2>Default vendor</h2>
 * <p>{@link #resolve(String)} returns the vendor whose prefix the name
 * carries, or {@link #ES} when no prefix is found. This makes untagged
 * (legacy) index names transparently route to Elasticsearch.</p>
 *
 * <h2>Tag-dispatch routing pattern</h2>
 * <pre>{@code
 * // Route a tagged index name to the correct provider
 * IndexTag vendor = IndexTag.resolve(name);  // never null
 * T target = (vendor == IndexTag.OS) ? osImpl : esImpl;
 * target.someOperation(IndexTag.strip(name)); // strip before passing to provider
 * }</pre>
 *
 * <p>Use {@link #vendorOf(String)} instead of {@link #resolve(String)} when
 * you need to distinguish "explicitly tagged as ES" from "no tag present".</p>
 *
 * @see PhaseRouter
 */
public enum IndexTag {

    /** Marks an index name as originating from the OpenSearch backend. */
    OS("os::"),

    /** Marks an index name as originating from the legacy Elasticsearch backend. */
    ES("es::");

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /** The short prefix prepended to every tagged index name. */
    public final String prefix;

    IndexTag(final String prefix) {
        this.prefix = prefix;
    }

    // -------------------------------------------------------------------------
    // Instance operations
    // -------------------------------------------------------------------------

    /**
     * Prepends this vendor's prefix to {@code indexName}.
     *
     * <p>Idempotent: if {@code indexName} is already tagged with <em>any</em>
     * vendor prefix it is returned unchanged, preventing double-tagging.</p>
     *
     * @param indexName raw or already-tagged index name; {@code null} returns {@code null}
     * @return tagged name, e.g. {@code "os::cluster_live_20240315"}
     */
    public String tag(final String indexName) {
        if (indexName == null) {
            return null;
        }
        // Already tagged — do not stack prefixes
        for (final IndexTag existing : values()) {
            if (existing.isTagged(indexName)) {
                return indexName;
            }
        }
        return prefix + indexName;
    }

    /**
     * Removes this vendor's prefix from {@code taggedName}.
     *
     * <p>If the name does not start with this prefix it is returned unchanged —
     * no exception is thrown. Use {@link #isTagged(String)} to guard first
     * when strict matching is required.</p>
     *
     * @param taggedName tagged or untagged name; {@code null} returns {@code null}
     * @return the name without the vendor prefix
     */
    public String untag(final String taggedName) {
        if (taggedName != null && taggedName.startsWith(prefix)) {
            return taggedName.substring(prefix.length());
        }
        return taggedName;
    }

    /**
     * Returns {@code true} when {@code name} carries this vendor's prefix.
     *
     * @param name any string; {@code null} returns {@code false}
     */
    public boolean isTagged(final String name) {
        return name != null && name.startsWith(prefix);
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
     * the name carries no recognised prefix.
     *
     * <p>Use this in routing code where an untagged name must be treated as a
     * legacy Elasticsearch index:</p>
     * <pre>{@code
     * IndexTag vendor = IndexTag.resolve(name); // never null
     * String raw = vendor.untag(name);
     * }</pre>
     *
     * @param name any index name, tagged or untagged; {@code null} returns {@link #ES}
     * @return the vendor whose prefix the name carries, or {@link #ES} if none
     */
    public static IndexTag resolve(final String name) {
        return vendorOf(name).orElse(ES);
    }

    /**
     * Strips any known vendor prefix from {@code name}.
     *
     * <p>If no prefix matches the name is returned unchanged, making this safe
     * to call on already-untagged strings.</p>
     *
     * @param name tagged or raw index name; {@code null} returns {@code null}
     * @return the raw index name without any vendor prefix
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
