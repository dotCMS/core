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
    cwd: string;
    /** Confirmation. `--yes` must pass `() => true` — the SAFE answer, not a skip (FR-023). */
    confirmExclude?: (files: string[]) => Promise<boolean>;
}

/**
 * Files that projects conventionally COMMIT. `.mcp.json` at a repository root is shared team
 * configuration, so excluding it is the unusual choice — the one place where our safe default
 * is actively wrong for the developer's workflow, and the reason FR-024 demands an explicit
 * warning rather than silently adding it to `.gitignore`.
 */
const CONVENTIONALLY_COMMITTED = new Set(['.mcp.json']);

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

    for (const file of args.files) {
        if (CONVENTIONALLY_COMMITTED.has(path.basename(file))) {
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
