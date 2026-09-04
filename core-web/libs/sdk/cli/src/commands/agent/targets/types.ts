import type { Scope } from '../../../shared/types';

/** The seven supported editors. A union rather than `string` so an unknown id is a compile
 *  error as well as a named runtime error (FR-032). */
export type TargetId =
    | 'claude-code'
    | 'cursor'
    | 'vscode'
    | 'codex'
    | 'antigravity'
    | 'devin'
    | 'opencode';

/**
 * One entry per editor — the ONLY place an editor is described.
 *
 * `setup.ts` branches on none of these fields (FR-013) and `shared/` never learns that editors
 * exist, so adding an eighth is one object literal.
 */
export interface AgentTarget {
    id: TargetId;
    displayName: string;
    /** The `npx skills -a` id. `null` means skills are not installable here, which the summary
     *  must reflect rather than imply success (FR-027). */
    skillsAgentId: string | null;
    /**
     * Has this editor been CONFIRMED to read the directory the skills installer writes to?
     *
     * A field rather than a hardcoded exception, so FR-027 stays meaningful for a future target
     * whose location is only documented, and so adding an editor remains one object (FR-013).
     */
    skillsLocationVerified: boolean;
    format: 'json' | 'toml';
    /** `mcpServers` for most, `servers` for VS Code, `mcp` for OpenCode, `mcp_servers` for Codex. */
    containerKey: string;
    /** OpenCode's entry differs structurally, not merely by key. */
    entryShape: 'stdio' | 'opencode-local';
    /** Advisory only — an undetected editor is still explicitly selectable (spec Edge Cases). */
    detect(): Promise<boolean>;
    /**
     * `null` when the target has no file at that scope.
     *
     * `cwd` is injected rather than read from `process.cwd()` so folder scope is testable
     * without `process.chdir()` — mutating global process state inside a test leaks between
     * cases and is fragile if a case throws before cleanup.
     */
    configPath(scope: Scope, cwd?: string): string | null;
}
