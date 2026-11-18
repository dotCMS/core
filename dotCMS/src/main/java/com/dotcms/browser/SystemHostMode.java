package com.dotcms.browser;

/**
 * What a listing does about System Host content.
 *
 * <p>Three states rather than a flag, because the host predicate has three shapes and a boolean
 * can only name two of them. {@link #ONLY} is the one it could never say, and it is what lets
 * Content Drive browse shared content on its own.</p>
 *
 * <p><b>{@link #EXCLUDE} is the default, and that is load-bearing.</b> It reproduces exactly what
 * the boolean {@code false} produced before this existed. Several callers reach this builder
 * without ever mentioning System Host — the assets API, the older file browser and its deprecated
 * tree endpoint, the legacy admin browser, a Velocity viewtool, and two internal callers — so any
 * other default would silently change what all of them return.</p>
 */
public enum SystemHostMode {

    /** The named site only. What a caller that says nothing about System Host gets. */
    EXCLUDE,

    /** The named site, plus System Host alongside it. */
    INCLUDE,

    /** System Host alone. The named site becomes context rather than a filter. */
    ONLY
}
