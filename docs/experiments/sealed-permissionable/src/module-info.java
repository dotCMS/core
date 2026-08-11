/**
 * The one thing that unlocks everything else.
 *
 * <p>Delete this file and the very same sources stop compiling: a sealed type in the unnamed module
 * requires every permitted subtype to live in the same package, and these live in fourteen packages.
 * Inside a named module, {@code permits} may cross packages. That is the whole difference between
 * experiment 1 and experiment 2 — and the price tag on the whole idea.</p>
 *
 * <p>The two exports mirror {@code osgi-extra.conf}, which publishes both packages to plugin bundles.
 * Experiment 8 is about what sealing does to that promise.</p>
 */
module dotcms.permissions {
    exports com.dotmarketing.business;
    exports com.dotmarketing.beans;
}
