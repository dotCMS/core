import { existsSync } from 'node:fs';
import * as fs from 'node:fs/promises';
import * as path from 'node:path';

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
    let current = '';
    try {
        current = await fs.readFile(gitignorePath, 'utf8');
    } catch {
        /* no .gitignore yet */
    }

    const already = new Set(current.split('\n').map((line) => line.trim()));
    const toAdd = args.files
        .map((file) => path.relative(root, file).split(path.sep).join('/'))
        .filter((entry) => !already.has(entry));

    if (toAdd.length) {
        const prefix = current === '' || current.endsWith('\n') ? '' : '\n';
        const block = `${prefix}\n# dotCMS agent configuration — contains an access token\n${toAdd.join('\n')}\n`;
        await fs.writeFile(gitignorePath, current + block, 'utf8');
    }

    return { files: args.files, inRepository: true, excluded: true, warnings };
}
