import { parse } from 'smol-toml';

import * as fs from 'node:fs/promises';
import * as path from 'node:path';


import { buildEntry } from './json-target';

import { ensureDir, restrictFile } from '../../../shared/config-file';
import { MalformedConfigError, NoConfigPathError } from '../../../shared/errors';
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
/**
 * The line span of our own tables — `[mcp_servers.dotcms]` and any sub-table of it.
 *
 * Returns null when the entry is not present. The span runs from our header to the next header
 * that is not ours, so anything the developer wrote around it is untouched.
 */
function findEntrySpan(lines: string[], containerKey: string): { start: number; end: number } | null {
    const ours = new RegExp(`^\\s*\\[\\s*${containerKey}\\.${ENTRY_KEY}\\s*(\\.[^\\]]+)?\\]`);
    const anyHeader = /^\s*\[/;

    const start = lines.findIndex((line) => ours.test(line));
    if (start === -1) return null;

    let end = lines.length;
    for (let i = start + 1; i < lines.length; i++) {
        if (anyHeader.test(lines[i]) && !ours.test(lines[i])) {
            end = i;
            break;
        }
    }
    // Blank lines AND comments immediately before the next header belong to that header, not
    // to us. Without this the splice ate the comment introducing the following table.
    while (
        end > start + 1 &&
        (lines[end - 1].trim() === '' || lines[end - 1].trim().startsWith('#'))
    ) {
        end--;
    }
    return { start, end };
}

/** Our tables, rendered. Only this text is ever generated; the rest of the file is the user's. */
function renderEntry(target: AgentTarget, url: string, token: string, containerKey: string): string {
    const entry = buildEntry(target, url, token) as {
        command: string;
        args: string[];
        env: Record<string, string>;
    };
    const args = entry.args.map((a) => JSON.stringify(a)).join(', ');
    const env = Object.entries(entry.env)
        .map(([k, v]) => `${k} = ${JSON.stringify(v)}`)
        .join('\n');
    return (
        `[${containerKey}.${ENTRY_KEY}]\n` +
        `command = ${JSON.stringify(entry.command)}\n` +
        `args = [${args}]\n\n` +
        `[${containerKey}.${ENTRY_KEY}.env]\n${env}\n`
    );
}

export async function writeTomlTarget(args: TomlWriteArgs): Promise<string> {
    const file = args.target.configPath(args.scope, args.cwd);
    if (!file) {
        throw new NoConfigPathError(args.target.displayName, args.scope);
    }

    let original = '';
    try {
        original = await fs.readFile(file, 'utf8');
    } catch {
        /* absent file is the fresh-write case, not a failure */
    }

    // Parse to VALIDATE, not to rewrite. smol-toml discards comments on round-trip, and this
    // file is one a developer maintains by hand — regenerating it silently deleted their
    // annotations. So the parse guards against writing into a broken file (FR-018), and the
    // write itself splices text, leaving every byte outside our own tables exactly as it was.
    if (original.trim() !== '') {
        try {
            parse(original);
        } catch {
            throw new MalformedConfigError(file);
        }
    }

    const block = renderEntry(args.target, args.url, args.token, args.target.containerKey);
    let next: string;

    if (original.trim() === '') {
        next = block;
    } else {
        const lines = original.split('\n');
        const span = findEntrySpan(lines, args.target.containerKey);
        if (span) {
            lines.splice(span.start, span.end - span.start, ...block.trimEnd().split('\n'));
            next = lines.join('\n');
        } else {
            const sep = original.endsWith('\n') ? '\n' : '\n\n';
            next = `${original}${sep}${block}`;
        }
    }

    await ensureDir(path.dirname(file));
    await fs.writeFile(file, next.endsWith('\n') ? next : `${next}\n`, 'utf8');
    await restrictFile(file);
    return file;
}
