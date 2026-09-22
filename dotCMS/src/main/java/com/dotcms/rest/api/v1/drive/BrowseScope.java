package com.dotcms.rest.api.v1.drive;

/**
 * Which slice of content a Content Drive listing is asked for.
 *
 * <p>Named in full — <i>browse</i> scope — because Content Drive separately carries a
 * <i>search</i> scope that says which fields a text search reads. The two are independent: this
 * one says where you are, the other says how a search reads what is there.</p>
 *
 * <p><b>Absent is not a value here, and that is deliberate.</b> A request that omits the browse
 * scope keeps meaning exactly what it means today, which is what leaves the Asset Picker and every
 * other caller of this endpoint untouched. In particular {@link #ALL} is not the default: it is
 * meaningful only at the site root, and an omitted scope inside a folder is not the same thing as
 * {@code ALL} inside a folder.</p>
 */
public enum BrowseScope {

    /** The whole current site, at any depth. What the site root returns today. */
    ALL,

    /** Only the items that sit at the site root, not inside any folder. */
    ROOT,

    /** System Host content only. The site the request names is context, not a filter. */
    SYSTEM_HOST
}
