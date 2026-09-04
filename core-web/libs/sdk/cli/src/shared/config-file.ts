import * as fs from 'node:fs/promises';
import * as path from 'node:path';

import { MalformedConfigError } from './errors';

/** POSIX only. On Windows `chmod` toggles the read-only bit and never touches ACLs, so calling
 *  it there would return success while granting no protection — a false assurance is worse than
 *  the limitation (research R5). Callers report `permissionsApplied` accordingly. */
export const CAN_RESTRICT = process.platform !== 'win32';

const FILE_MODE = 0o600;
const DIR_MODE = 0o700;

export interface WriteResult {
    path: string;
    permissionsApplied: boolean;
    replacedExisting: boolean;
}

/** Read and parse, or return null when the file does not exist. Malformed input is a named
 *  error — never a silent overwrite (FR-018). */
export async function readJsonDocument(file: string): Promise<Record<string, unknown> | null> {
    let raw: string;
    try {
        raw = await fs.readFile(file, 'utf8');
    } catch {
        return null;
    }
    if (raw.trim() === '') return {};
    try {
        return JSON.parse(raw) as Record<string, unknown>;
    } catch {
        throw new MalformedConfigError(file);
    }
}

/**
 * Does this file already carry our entry?
 *
 * Read-only and deliberately separate from the write: FR-017's confirmation has to happen
 * before anything is modified, not as a rollback afterwards.
 */
export async function hasEntry(args: {
    file: string;
    containerKey: string;
    entryKey: string;
    parse?: (raw: string) => Record<string, unknown>;
}): Promise<boolean> {
    let doc: Record<string, unknown> | null;
    if (args.parse) {
        try {
            const raw = await fs.readFile(args.file, 'utf8');
            doc = raw.trim() === '' ? {} : args.parse(raw);
        } catch {
            return false;
        }
    } else {
        doc = await readJsonDocument(args.file);
    }
    const container = (doc?.[args.containerKey] as Record<string, unknown> | undefined) ?? {};
    return Object.prototype.hasOwnProperty.call(container, args.entryKey);
}

/** One place owns the file mode, so the JSON and TOML writers cannot drift apart on it. */
export async function restrictFile(file: string, canRestrict = CAN_RESTRICT): Promise<boolean> {
    if (!canRestrict) return false;
    await fs.chmod(file, FILE_MODE);
    return true;
}

/**
 * Create the directory if it is missing, and restrict ONLY what we created.
 *
 * Widening a directory the developer already owns is not ours to do: a `.cursor` deliberately
 * set to 0500 was silently reopened to 0700. `mkdir` tells us whether it existed — it returns
 * the first path created, or undefined when there was nothing to create.
 */
export async function ensureDir(dir: string): Promise<void> {
    const created = await fs.mkdir(dir, { recursive: true });
    if (created && CAN_RESTRICT) await fs.chmod(dir, DIR_MODE).catch(() => undefined);
}

/**
 * Merge one entry into a document the developer owns and write it back.
 *
 * Every other key survives exactly — this is User Story 2's P1 guarantee, and the reason the
 * whole file is parsed rather than text-spliced.
 */
export async function writeMerged(args: {
    file: string;
    containerKey: string;
    entryKey: string;
    entry: unknown;
    /** Injected so the honesty of `permissionsApplied` is testable on a platform that CAN
     *  restrict — otherwise the assertion is `true === true` and a hard-coded claim passes. */
    canRestrict?: boolean;
}): Promise<WriteResult> {
    const existing = (await readJsonDocument(args.file)) ?? {};
    const container = (existing[args.containerKey] as Record<string, unknown> | undefined) ?? {};
    const replacedExisting = Object.prototype.hasOwnProperty.call(container, args.entryKey);

    const next = {
        ...existing,
        [args.containerKey]: { ...container, [args.entryKey]: args.entry }
    };

    await ensureDir(path.dirname(args.file));
    await fs.writeFile(args.file, `${JSON.stringify(next, null, 2)}\n`, 'utf8');
    const permissionsApplied = await restrictFile(args.file, args.canRestrict ?? CAN_RESTRICT);
    return { path: args.file, permissionsApplied, replacedExisting };
}
