import type { Plugin } from 'vite';

/**
 * Serve every SCSS/Sass import as empty CSS during unit tests.
 *
 * WHY, in order of importance:
 *
 * 1. It removes a hang, not a slowdown. Vite reaches for a SCSS preprocessor and
 *    prefers `sass-embedded` whenever it is resolvable — and it is, because Vite
 *    itself depends on it (the workspace only declares plain `sass`). That package
 *    runs a `dart-sass/src/sass.snapshot --embedded` SUBPROCESS. It normally shuts
 *    down with the run, but not always: `portlets-content-drive:test` was observed
 *    sitting at 0% CPU with 1.4GB resident and a live sass child for 18 minutes,
 *    its JUnit report open at 0 bytes, never exiting. The dart process keeps the
 *    event loop alive, so the task never ends and `nx run-many` waits forever.
 *    Vite 7 has no user-facing option to choose plain `sass` — `skipEmbedded` is
 *    only an internal fallback for a broken native binary — so the reliable move is
 *    to never need a preprocessor at all. Measured: with this plugin, zero sass
 *    processes appear at any point during a run, so the shutdown race cannot
 *    happen. Without it, one appears on every run.
 *
 * 2. It restores Jest's behaviour rather than changing it. Jest routed every
 *    .css/.scss/.sass/.less import through `identity-obj-proxy`, so compiled CSS
 *    never existed in a unit test and no assertion could depend on it. Compiling
 *    SCSS under Vitest is work Jest never did.
 *
 * SCSS ONLY, deliberately: `*.module.css` still goes through Vite so
 * `css.modules.classNameStrategy` keeps working — sdk-react's Column spec asserts
 * `toHaveClass('col-start-2')` on a class read out of one.
 *
 * A project with no SCSS of its own still needs this: `server.deps.inline` pulls
 * sibling `libs/**` sources in, and those components carry .scss `styleUrls`.
 * content-drive has zero .scss files and still started sass on every run.
 */
const STUB_ID = '\0vitest-stub-scss.css';

export function stubScss(): Plugin {
    return {
        name: 'dotcms:vitest-stub-scss',
        // Ahead of Vite's own CSS handling, which is what would load the preprocessor.
        enforce: 'pre',
        resolveId(id) {
            return /\.s[ac]ss(\?.*)?$/.test(id) ? STUB_ID : null;
        },
        load(id) {
            return id === STUB_ID ? '' : null;
        }
    };
}
