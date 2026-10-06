import {
    type Node,
    findNodeAtLocation,
    parse as parseJsonc,
    parseTree,
    type ParseError
} from 'jsonc-parser';

import * as fs from 'node:fs/promises';
import * as path from 'node:path';

import { MalformedConfigError, UnreadableConfigError } from './errors';

/** POSIX only. On Windows `chmod` toggles the read-only bit and never touches ACLs, so calling
 *  it there would return success while granting no protection — a false assurance is worse than
 *  the limitation (research R5). Callers report `permissionsApplied` accordingly. */
export const CAN_RESTRICT = process.platform !== 'win32';

export const FILE_MODE = 0o600;
const DIR_MODE = 0o700;

export interface WriteResult {
    path: string;
    permissionsApplied: boolean;
    replacedExisting: boolean;
}

/**
 * Read a file, distinguishing "not there" from "could not read it".
 *
 * Every read site used a bare `catch { null }`, so EACCES, EISDIR and a transient I/O error all
 * became "does not exist yet" — and the fresh-file branch downstream then REPLACED a file that
 * was merely locked, discarding every other MCP server in it. Same outcome the byte-preservation
 * work exists to prevent, reached through a different door.
 */
export async function readFileIfPresent(file: string): Promise<string | null> {
    try {
        return await fs.readFile(file, 'utf8');
    } catch (error) {
        if ((error as NodeJS.ErrnoException).code === 'ENOENT') return null;
        throw new UnreadableConfigError(file, (error as Error).message);
    }
}

/**
 * Write by creating a sibling temp file and renaming over the target.
 *
 * `fs.writeFile` truncates first: a crash, a full disk or a SIGINT between truncate and write
 * leaves the developer's config empty, taking every other server with it — undoing everything
 * parse-to-validate and splice-to-write are for. `rename` within a filesystem is atomic, so a
 * reader sees the old file or the new one, never a half of either. It also means the token bytes
 * are owner-only for their whole life on disk: `mode` is ignored for a file that already exists,
 * so a pre-existing 0644 config used to hold the token at 0644 until the chmod a line later.
 */
export async function writeFileAtomic(file: string, contents: string): Promise<void> {
    const temp = `${file}.dotcms-${process.pid}.tmp`;
    try {
        await fs.writeFile(temp, contents, { encoding: 'utf8', mode: FILE_MODE });
    } catch {
        // Renaming needs write permission on the DIRECTORY, which a plain overwrite does not.
        // Rather than fail a case that used to work, fall back to writing in place.
        await fs.writeFile(file, contents, { encoding: 'utf8', mode: FILE_MODE });
        return;
    }
    try {
        await fs.rename(temp, file);
    } catch (error) {
        await fs.rm(temp, { force: true });
        throw error;
    }
}

/**
 * Parse permissively, on purpose.
 *
 * `.vscode/mcp.json` is JSONC — the same family as `settings.json` and `launch.json`, and
 * VS Code's own docs show commented examples. `JSON.parse` rejected those files, so the tool
 * told developers a config their editor accepts was "not valid JSON" and refused to configure
 * the editor at all. Trailing commas failed the same way.
 *
 * Permissive is not lax: a genuine syntax error still raises MalformedConfigError (FR-018).
 */
const PARSE_OPTIONS = { allowTrailingComma: true, allowEmptyContent: true } as const;

function parseOrThrow(raw: string, file: string): Record<string, unknown> {
    if (raw.trim() === '') return {};
    const errors: ParseError[] = [];
    const doc = parseJsonc(raw, errors, PARSE_OPTIONS) as Record<string, unknown> | undefined;
    if (errors.length > 0 || doc === undefined) throw new MalformedConfigError(file);
    // `[1,2,3]`, `"text"`, `42` and `null` all parse cleanly. They used to fall through to the
    // fresh-file branch and REPLACE the developer's file wholesale; FR-018 says an input we
    // cannot merge into is a named error that leaves the file untouched.
    if (typeof doc !== 'object' || doc === null || Array.isArray(doc)) {
        throw new MalformedConfigError(file);
    }
    return doc;
}

/** Read and parse, or return null when the file does not exist. Malformed input is a named
 *  error — never a silent overwrite (FR-018). */
export async function readJsonDocument(file: string): Promise<Record<string, unknown> | null> {
    const raw = await readFileIfPresent(file);
    return raw === null ? null : parseOrThrow(raw, file);
}

/**
 * The indentation the file already uses.
 *
 * Imposing two spaces on a four-space file makes our entry the odd one out in a document we
 * do not own. Read the first indented line and follow it.
 */
export function detectIndent(raw: string): { insertSpaces: boolean; tabSize: number } {
    const match = raw.match(/\n([ \t]+)\S/);
    if (!match) return { insertSpaces: true, tabSize: 2 };
    const indent = match[1];
    return indent.startsWith('\t')
        ? { insertSpaces: false, tabSize: 1 }
        : { insertSpaces: true, tabSize: indent.length };
}

/**
 * Does this file already carry our entry?
 *
 * Read-only and deliberately separate from the write: FR-017's confirmation has to happen
 * before anything is modified, not as a rollback afterwards.
 *
 * JSON only. This used to take an injected `parse` so the flow could hand it a TOML parser,
 * which is how `shared/` came to know a second format existed — and it left TOML with two
 * disagreeing notions of "present". Each writer now answers for its own format.
 */
export async function hasEntry(args: {
    file: string;
    containerKey: string;
    entryKey: string;
}): Promise<boolean> {
    const doc = await readJsonDocument(args.file).catch(() => null);
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
 * Render `value` as JSON indented to sit at `depth` levels inside the document.
 *
 * `JSON.stringify` always indents from column zero, so every line after the first has to be
 * pushed out to where it actually lives.
 */
function renderAt(value: unknown, unit: string, depth: number): string {
    const base = unit.repeat(depth);
    return JSON.stringify(value, null, unit)
        .split('\n')
        .map((line, i) => (i === 0 ? line : base + line))
        .join('\n');
}

/**
 * Set one property on an object node by splicing text, never by re-serializing the document.
 *
 * This is the JSON counterpart of what `toml-target.ts` already does. `jsonc-parser`'s own
 * `modify` was the obvious tool and is the wrong one here: with `formattingOptions` it reflows
 * the sibling its insertion point abuts, and without them it emits the entry compacted onto a
 * single line. Both rewrite bytes we were asked to leave alone (FR-016). Computing the one
 * offset ourselves is a dozen lines and touches nothing else.
 *
 * Returns the edited text.
 */
function setProperty(
    raw: string,
    object: Node,
    key: string,
    value: unknown,
    unit: string,
    depth: number
): string {
    const rendered = renderAt(value, unit, depth);
    const properties = object.children ?? [];

    // Replace the VALUE only, so the existing key and its formatting stay as written.
    const existing = properties.find((p) => p.children?.[0]?.value === key);
    if (existing?.children?.[1]) {
        const node = existing.children[1];
        return raw.slice(0, node.offset) + rendered + raw.slice(node.offset + node.length);
    }

    const insertion = `"${key}": ${rendered}`;
    const last = properties[properties.length - 1];
    if (last) {
        const end = last.offset + last.length;
        return `${raw.slice(0, end)},\n${unit.repeat(depth)}${insertion}${raw.slice(end)}`;
    }

    // An empty container: `{}` or `{ }`. Open it up rather than guessing at its interior.
    const close = raw.lastIndexOf('}', object.offset + object.length);
    return (
        `${raw.slice(0, object.offset + 1)}\n${unit.repeat(depth)}${insertion}\n` +
        `${unit.repeat(depth - 1)}${raw.slice(close)}`
    );
}

/**
 * Merge one entry into a document the developer owns and write it back.
 *
 * Every other key survives byte-for-byte — User Story 2's P1 guarantee, and the reason the file
 * is parsed to VALIDATE and spliced to WRITE rather than re-serialized from its parse tree.
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
    const raw = await readFileIfPresent(args.file);

    const existing = raw === null ? {} : parseOrThrow(raw, args.file);
    const container = (existing[args.containerKey] as Record<string, unknown> | undefined) ?? {};
    const replacedExisting = Object.prototype.hasOwnProperty.call(container, args.entryKey);

    // A brand-new file has no formatting to preserve, so serialize it outright. An existing
    // one is EDITED, never rebuilt: only the bytes of our own key change (FR-016).
    let next: string;
    const tree = raw === null ? undefined : parseTree(raw, [], PARSE_OPTIONS);
    if (raw === null || raw.trim() === '' || tree?.type !== 'object') {
        next = `${JSON.stringify({ [args.containerKey]: { [args.entryKey]: args.entry } }, null, 2)}\n`;
    } else {
        const { insertSpaces, tabSize } = detectIndent(raw);
        const unit = insertSpaces ? ' '.repeat(tabSize) : '\t';
        const containerNode = findNodeAtLocation(tree, [args.containerKey]);
        next =
            containerNode?.type === 'object'
                ? setProperty(raw, containerNode, args.entryKey, args.entry, unit, 2)
                : setProperty(
                      raw,
                      tree,
                      args.containerKey,
                      { [args.entryKey]: args.entry },
                      unit,
                      1
                  );
    }

    await ensureDir(path.dirname(args.file));
    await writeFileAtomic(args.file, next);
    const permissionsApplied = await restrictFile(args.file, args.canRestrict ?? CAN_RESTRICT);
    return { path: args.file, permissionsApplied, replacedExisting };
}
