// A NAMED import, deliberately: a default import makes esbuild inline the whole manifest
// into the shipped bundle — dependency list, publishConfig and all. This tree-shakes to the
// one string.
import { version } from '../../package.json';

/**
 * The version this tool shipped with, read from `package.json` at BUILD time.
 *
 * Never hardcoded: the SDK release pipeline rewrites `.version` to the dotCMS release tag
 * *before* it builds, so reading the manifest is what keeps the compatibility warning honest
 * once published. A literal here would report `0.2.0` from a package published as `26.9.x`.
 *
 * Pre-release that value is `0.2.0`, which is lower than any dotCMS release version — so the
 * comparison is silent rather than wrong, which is the right failure direction for a warning
 * that must never block a run (FR-005a).
 */
export const TOOL_VERSION: string = version;
