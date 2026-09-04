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

export async function ensureDir(dir: string): Promise<void> {
    await fs.mkdir(dir, { recursive: true });
    if (CAN_RESTRICT) await fs.chmod(dir, DIR_MODE).catch(() => undefined);
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
    let permissionsApplied = false;
    if (CAN_RESTRICT) {
        await fs.chmod(args.file, FILE_MODE);
        permissionsApplied = true;
    }
    return { path: args.file, permissionsApplied, replacedExisting };
}
