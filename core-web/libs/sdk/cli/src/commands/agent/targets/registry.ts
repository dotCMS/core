import { existsSync } from 'node:fs';
import * as os from 'node:os';
import * as path from 'node:path';

import { ENV_KEYS, readEnv } from '../../../shared/env';
import { UnknownTargetError } from '../../../shared/errors';

import type { AgentTarget, TargetId } from './types';
import type { Scope } from '../../../shared/types';

const home = () => os.homedir();
const inHome = (...parts: string[]) => path.join(home(), ...parts);
const inFolder = (cwd: string | undefined, ...parts: string[]) =>
    path.join(cwd ?? process.cwd(), ...parts);

/** Advisory only — an undetected editor stays explicitly selectable (spec Edge Cases). */
const probe = (...parts: string[]) => async () => existsSync(inHome(...parts));

/** VS Code's user directory is different on all three platforms. */
function vscodeUserDir(): string {
    if (process.platform === 'darwin') return inHome('Library', 'Application Support', 'Code', 'User');
    if (process.platform === 'win32') {
        const appData = process.env['APPDATA'] ?? inHome('AppData', 'Roaming');
        return path.join(appData, 'Code', 'User');
    }
    return inHome('.config', 'Code', 'User');
}

/** `$CODEX_HOME` overrides the default location when set. */
function codexHome(): string {
    return readEnv(ENV_KEYS.codexHome) ?? inHome('.codex');
}

/**
 * The seven supported editors — the ONLY place an editor is described.
 *
 * Adding an eighth is one object literal here and no change anywhere else (FR-013). Nothing in
 * `shared/` knows that editors exist.
 */
export const TARGETS: readonly AgentTarget[] = [
    {
        id: 'claude-code',
        displayName: 'Claude Code',
        skillsAgentId: 'claude-code',
        skillsLocationVerified: true,
        format: 'json',
        containerKey: 'mcpServers',
        entryShape: 'stdio',
        detect: probe('.claude'),
        configPath: (scope: Scope, cwd?: string) =>
            scope === 'global' ? inHome('.claude.json') : inFolder(cwd, '.mcp.json')
    },
    {
        id: 'cursor',
        displayName: 'Cursor',
        skillsAgentId: 'cursor',
        skillsLocationVerified: true,
        format: 'json',
        containerKey: 'mcpServers',
        entryShape: 'stdio',
        detect: probe('.cursor'),
        configPath: (scope: Scope, cwd?: string) =>
            scope === 'global' ? inHome('.cursor', 'mcp.json') : inFolder(cwd, '.cursor', 'mcp.json')
    },
    {
        id: 'vscode',
        displayName: 'VS Code (Copilot)',
        // Writes to ~/.copilot/skills — the Copilot CLI location. Not confirmed to be read by
        // the in-editor agent, so the summary must not claim skills landed here (FR-027).
        skillsAgentId: 'github-copilot',
        // Confirmed 2026-09-04 against VS Code's own docs: the in-editor agent reads Agent
        // Skills from ~/.copilot/skills (alongside ~/.claude/skills and ~/.agents/skills), the
        // same directory `skills -a github-copilot` writes to. The spec's caveat is retired.
        skillsLocationVerified: true,
        format: 'json',
        // NOT `mcpServers` — VS Code is the one that differs by key.
        containerKey: 'servers',
        entryShape: 'stdio',
        detect: async () => existsSync(vscodeUserDir()),
        configPath: (scope: Scope, cwd?: string) =>
            scope === 'global'
                ? path.join(vscodeUserDir(), 'mcp.json')
                : inFolder(cwd, '.vscode', 'mcp.json')
    },
    {
        id: 'codex',
        displayName: 'Codex',
        skillsAgentId: 'codex',
        skillsLocationVerified: true,
        format: 'toml',
        containerKey: 'mcp_servers',
        entryShape: 'stdio',
        detect: probe('.codex'),
        configPath: (scope: Scope, cwd?: string) =>
            scope === 'global'
                ? path.join(codexHome(), 'config.toml')
                : inFolder(cwd, '.codex', 'config.toml')
    },
    {
        id: 'antigravity',
        displayName: 'Antigravity',
        skillsAgentId: 'antigravity',
        skillsLocationVerified: true,
        format: 'json',
        containerKey: 'mcpServers',
        entryShape: 'stdio',
        detect: probe('.gemini'),
        configPath: (scope: Scope, cwd?: string) =>
            scope === 'global'
                ? inHome('.gemini', 'config', 'mcp_config.json')
                : inFolder(cwd, '.agents', 'mcp_config.json')
    },
    {
        id: 'devin',
        displayName: 'Devin',
        skillsAgentId: 'devin',
        skillsLocationVerified: true,
        format: 'json',
        containerKey: 'mcpServers',
        entryShape: 'stdio',
        detect: probe('.config', 'devin'),
        configPath: (scope: Scope, cwd?: string) =>
            scope === 'global'
                ? inHome('.config', 'devin', 'mcp_config.json')
                : inFolder(cwd, '.devin', 'mcp_config.local.json')
    },
    {
        id: 'opencode',
        displayName: 'OpenCode',
        skillsAgentId: 'opencode',
        skillsLocationVerified: true,
        format: 'json',
        // Differs by key AND by entry shape — see `entryShape`.
        containerKey: 'mcp',
        entryShape: 'opencode-local',
        detect: probe('.config', 'opencode'),
        configPath: (scope: Scope, cwd?: string) =>
            scope === 'global'
                ? inHome('.config', 'opencode', 'opencode.json')
                : inFolder(cwd, 'opencode.json')
    }
];

export const TARGET_IDS: readonly TargetId[] = TARGETS.map((t) => t.id);

export function getTarget(id: TargetId): AgentTarget {
    const found = TARGETS.find((t) => t.id === id);
    if (!found) throw new UnknownTargetError(id, TARGET_IDS);
    return found;
}

export async function detectTargets(): Promise<AgentTarget[]> {
    const results = await Promise.all(TARGETS.map(async (t) => ((await t.detect()) ? t : null)));
    return results.filter((t): t is AgentTarget => t !== null);
}
