import { parse, stringify } from 'smol-toml';

import * as fs from 'node:fs/promises';
import * as path from 'node:path';


import { buildEntry } from './json-target';

import { ensureDir, restrictFile } from '../../../shared/config-file';
import { MalformedConfigError } from '../../../shared/errors';
import { ENTRY_KEY } from '../constants';

import type { AgentTarget } from './types';
import type { Scope } from '../../../shared/types';

export interface TomlWriteArgs {
    target: AgentTarget;
    scope: Scope;
    url: string;
    token: string;
    cwd?: string;
}

/**
 * Codex is the only non-JSON target.
 *
 * The file belongs to the developer and will carry unrelated tables and comments, so it is
 * parsed and re-serialized rather than text-spliced: replacing an existing
 * `[mcp_servers.dotcms]` in place requires understanding where a table begins and ends, which
 * is parsing by another name (research R6).
 */
export async function writeTomlTarget(args: TomlWriteArgs): Promise<string> {
    const file = args.target.configPath(args.scope, args.cwd);
    if (!file) {
        throw new Error(`${args.target.displayName} has no configuration file at ${args.scope} scope.`);
    }

    let existing: Record<string, unknown> = {};
    try {
        const raw = await fs.readFile(file, 'utf8');
        if (raw.trim() !== '') {
            try {
                existing = parse(raw) as Record<string, unknown>;
            } catch {
                // Named error, original untouched, never overwritten (FR-018).
                throw new MalformedConfigError(file);
            }
        }
    } catch (error) {
        if (error instanceof MalformedConfigError) throw error;
        // Absent file is the fresh-write case, not a failure.
    }

    const container =
        (existing[args.target.containerKey] as Record<string, unknown> | undefined) ?? {};

    const next = {
        ...existing,
        [args.target.containerKey]: {
            ...container,
            [ENTRY_KEY]: buildEntry(args.target, args.url, args.token)
        }
    };

    await ensureDir(path.dirname(file));
    await fs.writeFile(file, `${stringify(next)}\n`, 'utf8');
    await restrictFile(file);
    return file;
}
