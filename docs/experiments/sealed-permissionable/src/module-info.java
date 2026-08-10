/**
 * The one thing that unlocks everything below.
 *
 * <p>Delete this file and the very same sources stop compiling: a sealed type in the unnamed module
 * requires every permitted subtype to live in the same package. Inside a named module, `permits` may
 * cross packages. That is the whole difference between experiment 1 and experiment 2.</p>
 */
module dotcms.permissions {
    exports com.dotmarketing.business;
}
