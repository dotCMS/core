package com.dotcms.content.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Vendor-origin tag for raw index-name strings.
 *
 * <h2>Purpose</h2>
 * <p>When flat {@code String} index names from the OpenSearch backend
 * ({@code VersionedIndicesAPI}) and the legacy Elasticsearch backend
 * ({@code IndiciesInfo}) are mixed in the same collection there is no
 * structural way to tell them apart. {@code IndexNameTag} solves this by
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
 * <h2>Idempotency</h2>
 * <p>{@link #tag(String)} is idempotent: calling it on a name that already
 * carries <em>any</em> vendor prefix leaves the name unchanged, preventing
 * accidental double-tagging.</p>
 *
 * <h2>Default vendor</h2>
 * <p>{@link #resolve(String)} returns the vendor whose prefix the name
 * carries, or {@link #ES} when no prefix is found. This makes untagged
 * (legacy) index names transparently route to Elasticsearch.</p>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * // Tag OS indices loaded from VersionedIndicesAPI
 * List<String> osTagged = IndexNameTag.OS.tagAll(versionedIndices.osIndices());
 *
 * // Tag ES indices loaded from legacy IndiciesInfo
 * List<String> esTagged = IndexNameTag.ES.tagAll(indiciesInfoNames);
 *
 * // Route each name to the correct delegate
 * Stream.concat(osTagged.stream(), esTagged.stream()).forEach(name -> {
 *     IndexNameTag vendor = IndexNameTag.resolve(name); // never null
 *     String raw = vendor.untag(name);                  // strips the prefix
 *     // … delegate to osImpl or esImpl based on vendor …
 * });
 * }</pre>
 *
 * <p>Use {@link #vendorOf(String)} instead of {@code resolve} when you need
 * to distinguish "explicitly tagged as ES" from "no tag present".</p>
 *
 * @author Fabrizzio Araya
 * @see com.dotcms.content.index.VersionedIndicesImpl
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
     * Returns a new list with every element in {@code names} tagged by this vendor.
     *
     * @param names source collection; {@code null} returns an empty list
     * @return mutable list of tagged names
     */
    public List<String> tagAll(final Collection<String> names) {
        if (names == null) {
            return new ArrayList<>();
        }
        final List<String> result = new ArrayList<>(names.size());
        for (final String name : names) {
            result.add(tag(name));
        }
        return result;
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
     * @param name tagged or raw index name; {@code null} returns empty
     * @return the matching {@code IndexNameTag}, or {@link Optional#empty()}
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
     * <p>Use this method in routing code where an untagged name must be treated
     * as a legacy Elasticsearch index:</p>
     * <pre>{@code
     * IndexNameTag vendor = IndexNameTag.resolve(name); // never null
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
     * Resolves the vendor tag and strips the prefix in a single call.
     *
     * <pre>{@code
     * Tagged t = IndexNameTag.parse("os::contentlet_live_20240315");
     * t.tag()  // → OS
     * t.name() // → "contentlet_live_20240315"
     *
     * Tagged t2 = IndexNameTag.parse("cluster_live_20240315"); // no prefix
     * t2.tag()  // → ES  (legacy default)
     * t2.name() // → "cluster_live_20240315"
     * }</pre>
     *
     * @param name tagged or untagged index name; {@code null} is treated as untagged
     * @return a {@link Tagged} holding the resolved vendor and the raw name
     */
    public static Tagged parse(final String name) {
        final IndexTag tag = resolve(name);
        return new Tagged(tag, tag.untag(name));
    }

    /**
     * Pair of a resolved {@link IndexTag} and the corresponding raw index name
     * (prefix already stripped).
     *
     * <p>Returned by {@link IndexTag#parse(String)}.</p>
     */
    public static final class Tagged {

        private final IndexTag tag;
        private final String name;

        private Tagged(final IndexTag tag, final String name) {
            this.tag  = tag;
            this.name = name;
        }

        /** The vendor that owns this index ({@link IndexTag#OS} or {@link IndexTag#ES}). */
        public IndexTag tag()  { return tag; }

        /** The raw index name with the vendor prefix removed. */
        public String name() { return name; }

        @Override
        public String toString() {
            return tag.prefix + name;
        }
    }

    /**
     * Tags {@code indexName} with the given {@code vendor} prefix.
     *
     * <p>Delegates to {@link #tag(String)} on the supplied vendor, so the
     * call is idempotent: a name that already carries <em>any</em> vendor
     * prefix is returned unchanged.</p>
     *
     * @param indexName raw or already-tagged index name; {@code null} returns {@code null}
     * @param vendor    the vendor prefix to apply; must not be {@code null}
     * @return tagged name, e.g. {@code "os::contentlet_live_20240315"}
     */
    public static String tagWith(final String indexName, final IndexTag vendor) {
        return vendor.tag(indexName);
    }

    /**
     * Tags {@code indexName} with the {@link #OS} prefix (default vendor).
     *
     * <p>Equivalent to {@code IndexTag.tagWith(indexName, IndexTag.OS)}.</p>
     *
     * @param indexName raw or already-tagged index name; {@code null} returns {@code null}
     * @return tagged name, e.g. {@code "os::contentlet_live_20240315"}
     */
    public static String tagWith(final String indexName) {
        return tagWith(indexName, OS);
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