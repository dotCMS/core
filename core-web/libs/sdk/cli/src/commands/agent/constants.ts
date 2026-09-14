/**
 * The canonical key for the entry this tool writes. Exactly one per configuration file
 * (FR-011b); a later run finds and replaces precisely this key and nothing else.
 */
export const ENTRY_KEY = 'dotcms';

/**
 * Referenced unpinned, so server updates reach developers without re-running setup (FR-020e).
 *
 * This is a deliberate deviation from ADR-0019's pin-exact-versions guidance: `@dotcms/mcp-server`
 * is versioned independently of dotCMS and has no release workflow in this repo, so there is no
 * CLI-matched version to pin to. See plan.md Complexity Tracking.
 */
export const MCP_SERVER_PACKAGE = '@dotcms/mcp-server@latest';

/**
 * Exactly the names `runtimeFromEnv()` reads in apps/mcp-server/src/lib/runtime.ts.
 * NOT `DOTCMS_TOKEN` — that mistake yields a server that starts and then fails every call,
 * which is the failure the connection check (FR-024a) exists to catch.
 */
export const SERVER_ENV = {
    url: 'DOTCMS_URL',
    token: 'AUTH_TOKEN'
} as const;

/** The skills toolkit, delegated to the `skills` CLI. Public repository (verified). */
export const SKILLS_SOURCE = 'dotCMS/agent-toolkit';
