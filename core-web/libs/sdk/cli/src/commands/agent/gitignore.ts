import { spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import * as fs from 'node:fs/promises';
import * as path from 'node:path';

import { readFileIfPresent } from '../../shared/config-file';

export interface GitignoreOutcome {
    /** Files a token was written into, always named regardless of what we could do about them. */
    files: string[];
    inRepository: boolean;
    excluded: boolean;
    warnings: string[];
}

export interface GitignoreArgs {
    files: string[];
    /**
     * Of `files`, those the registry marks as conventionally committed. Supplied by the caller,
     * which knows which target wrote what — this module has no idea editors exist.
     */
    committedByConvention?: string[];
    cwd: string;
    /** Confirmation. `--yes` must pass `() => true` — the SAFE answer, not a skip (FR-023). */
    confirmExclude?: (files: string[]) => Promise<boolean>;
}

/**
 * A `.gitignore` entry is a GLOB, not a path.
 *
 * `path.relative` output went in raw, so a directory named `my [wip] app` produced a pattern
 * where `[wip]` is a character class — matching nothing, while we reported success. Anchored
 * with a leading `/` too: unanchored `.mcp.json` also matches at any depth.
 */
function asPattern(relative: string): string {
    const escaped = relative.replace(/([\\*?[\]])/g, '\\$1').replace(/ $/, '\\ ');
    return `/${escaped}`;
}

/** Which of `files` git does NOT consider ignored, or null when git could not answer. */
function confirmIgnored(root: string, files: string[]): string[] | null {
    const missed: string[] = [];
    for (const file of files) {
        const result = spawnSync('git', ['check-ignore', '-q', file], { cwd: root });
        if (result.error || result.status === null || result.status > 1) return null;
        if (result.status === 1) missed.push(path.basename(file));
    }
    return missed;
}

function findRepositoryRoot(from: string): string | null {
    let dir = path.resolve(from);
    for (;;) {
        if (existsSync(path.join(dir, '.git'))) return dir;
        const parent = path.dirname(dir);
        if (parent === dir) return null;
        dir = parent;
    }
}

/**
 * Name every file a token was written into, and offer to keep them out of version control.
 *
 * Folder scope is the DEFAULT (FR-011), so this runs on nearly every invocation rather than as
 * an edge case — which is why the files are named even when nothing can be done about them
 * (FR-023a). Silence here is how a token reaches a public repository.
 */
export async function protectFromVersionControl(args: GitignoreArgs): Promise<GitignoreOutcome> {
    const warnings: string[] = [];
    const root = findRepositoryRoot(args.cwd);

    const committed = new Set(args.committedByConvention ?? []);
    for (const file of args.files) {
        if (committed.has(file)) {
            warnings.push(
                `${path.basename(file)} is normally committed to version control — it now holds a ` +
                    `token, so committing it would publish that token.`
            );
        }
    }

    if (!root) {
        warnings.push(
            'This directory is not under version control, so these files are unprotected — ' +
                'nothing here can exclude them for you.'
        );
        return { files: args.files, inRepository: false, excluded: false, warnings };
    }

    const proceed = args.confirmExclude ? await args.confirmExclude(args.files) : false;
    if (!proceed) {
        return { files: args.files, inRepository: true, excluded: false, warnings };
    }

    const gitignorePath = path.join(root, '.gitignore');
    // Absent is fine; unreadable is not. `current` defaulting to '' on an EACCES meant the
    // unconditional write below replaced the developer's entire .gitignore with our block.
    const current = (await readFileIfPresent(gitignorePath)) ?? '';

    const already = new Set(current.split('\n').map((line) => line.trim()));
    const relatives = args.files.map((file) => path.relative(root, file).split(path.sep).join('/'));
    // Both spellings, so a re-run does not append a duplicate of an entry written before the
    // patterns were escaped.
    const toAdd = relatives.filter((rel) => !already.has(rel) && !already.has(asPattern(rel)));

    if (toAdd.length) {
        const prefix = current === '' || current.endsWith('\n') ? '' : '\n';
        const block = `${prefix}\n# dotCMS agent configuration — contains an access token\n${toAdd
            .map(asPattern)
            .join('\n')}\n`;
        await fs.writeFile(gitignorePath, current + block, 'utf8');
    }

    // Claiming exclusion without checking is the false assurance this module argues against, and
    // the failure is the bad direction: a pattern that matches nothing still printed
    // "✓ added to .gitignore" while the token file stayed tracked. Ask git.
    const unverified = confirmIgnored(root, args.files);
    if (unverified === null) {
        warnings.push(
            'Could not confirm with git that these files are excluded — check `git status` before committing.'
        );
    } else if (unverified.length) {
        warnings.push(
            `.gitignore was written but git still tracks: ${unverified.join(', ')}. Exclude them by hand before committing.`
        );
    }

    return {
        files: args.files,
        inRepository: true,
        excluded: unverified !== null && unverified.length === 0,
        warnings
    };
}
