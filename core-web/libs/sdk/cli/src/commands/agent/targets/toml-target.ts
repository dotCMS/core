import { parse, stringify } from 'smol-toml';

import * as fs from 'node:fs/promises';
import * as path from 'node:path';

import { buildEntry } from './entry';

import { ensureDir, restrictFile, type WriteResult } from '../../../shared/config-file';
import { MalformedConfigError, NoConfigPathError } from '../../../shared/errors';
import { ENTRY_KEY } from '../constants';

import type { AgentTarget, WriteArgs } from './types';

/**
 * Codex is the only non-JSON target.
 *
 * The file belongs to the developer and carries unrelated tables and comments, so it is parsed
 * to VALIDATE and spliced to WRITE — never re-serialized, because smol-toml drops comments on
 * round-trip and this is a file people maintain by hand (research R6).
 */

/**
 * Is our entry already present?
 *
 * Backed by `findEntrySpan`, deliberately: answering this with a TOML object-lookup while the
 * write located the entry by line span gave two notions of "present" that agreed only by luck.
 */
export async function hasTomlEntry(file: string, target: AgentTarget): Promise<boolean> {
    let raw: string;
    try {
        raw = await fs.readFile(file, 'utf8');
    } catch {
        return false;
    }
    return findEntrySpan(raw.split('\n'), target.containerKey) !== null;
}

/**
 * The line span of our own tables — `[mcp_servers.dotcms]` and any sub-table of it.
 *
 * Returns null when the entry is not present. The span runs from our header to the next header
 * that is not ours, so anything the developer wrote around it is untouched.
 */
function findEntrySpan(
    lines: string[],
    containerKey: string
): { start: number; end: number } | null {
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

/**
 * Our tables, rendered.
 *
 * Serialized generically from whatever `buildEntry` returns — NOT hand-emitted field by field.
 * The first version listed `command`, `args` and `env` explicitly behind a cast, which silently
 * dropped `type = "stdio"` and would have dropped any field added later with no compile error
 * and no failing test. Only this block is generated, so losing comments here costs nothing;
 * everything outside it is spliced, not re-serialized.
 */
function renderEntry(
    target: AgentTarget,
    url: string,
    token: string,
    containerKey: string
): string {
    return stringify({
        [containerKey]: { [ENTRY_KEY]: buildEntry(target, url, token) }
    }).trimEnd();
}

export async function writeTomlTarget(args: WriteArgs): Promise<WriteResult> {
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
            throw new MalformedConfigError(file, 'TOML');
        }
    }

    const block = renderEntry(args.target, args.url, args.token, args.target.containerKey);
    let next: string;
    let replacedExisting = false;

    if (original.trim() === '') {
        next = block;
    } else {
        const lines = original.split('\n');
        const span = findEntrySpan(lines, args.target.containerKey);
        if (span) {
            replacedExisting = true;
            lines.splice(span.start, span.end - span.start, ...block.trimEnd().split('\n'));
            next = lines.join('\n');
        } else {
            const sep = original.endsWith('\n') ? '\n' : '\n\n';
            next = `${original}${sep}${block}`;
        }
    }

    await ensureDir(path.dirname(file));
    await fs.writeFile(file, next.endsWith('\n') ? next : `${next}\n`, 'utf8');
    // Report what chmod actually did. The flow used to assert `CAN_RESTRICT` here, which is a
    // platform constant rather than an observation — the summary claimed a protection nobody
    // had checked.
    return { path: file, permissionsApplied: await restrictFile(file), replacedExisting };
}
