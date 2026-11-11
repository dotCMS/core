import { lstat, realpath } from 'node:fs/promises';
import { dirname, isAbsolute, relative, resolve, sep } from 'node:path';

import { ValidationError } from '../../../runtime';

/**
 * The local-filesystem boundary for the asset operations.
 *
 * `src` and `dest` come from the model. In the local stdio server that is the user's own agent
 * on the user's own machine, but in a hosted MCP server or a multi-user agent it is anyone who
 * can reach the tool — and without a boundary `upload_assets` reads any directory the process
 * can read (then readable back through `execute`) and `download_assets` writes into any
 * directory it can write. Every check here compares REAL paths, after symlinks resolve: a
 * symlink inside the root pointing out of it is outside, and `/tmp` is `/private/tmp` on macOS.
 */

function isInside(root: string, target: string): boolean {
    const back = relative(root, target);

    return !(back === '..' || back.startsWith(`..${sep}`) || isAbsolute(back));
}

/**
 * The real path of `path`, or of its nearest existing ancestor when it does not exist yet. The
 * tail that does not exist cannot contain a symlink, so the ancestor decides containment —
 * which is what lets a check run BEFORE `mkdir` creates anything.
 */
async function realpathOfNearest(path: string): Promise<string> {
    let current = resolve(path);
    for (;;) {
        try {
            return await realpath(current);
        } catch (error) {
            const parent = dirname(current);
            if ((error as NodeJS.ErrnoException).code !== 'ENOENT' || parent === current) {
                throw error;
            }
            current = parent;
        }
    }
}

/**
 * Refuse `path` when it resolves outside `root`. No `root` means no boundary — for trusted,
 * direct callers of the operations; the tools always set one.
 */
export async function assertInsideRoot(
    root: string | undefined,
    label: 'src' | 'dest',
    path: string
): Promise<void> {
    if (root === undefined) {
        return;
    }

    const rootReal = await realpath(resolve(root));
    if (!isInside(rootReal, await realpathOfNearest(path))) {
        throw new ValidationError(
            `\`${label}\` must be inside ${rootReal}, the directory this tool is allowed to use, ` +
                `and ${path} is not (symlinks are resolved before checking).`
        );
    }
}

/**
 * Refuse to write `file` when it would land outside `root`: its directory resolves elsewhere,
 * or the file itself is a symlink — `writeFile` follows one, so a link planted inside the
 * destination would otherwise redirect the write anywhere.
 */
export async function assertWritableInsideRoot(
    root: string | undefined,
    file: string
): Promise<void> {
    if (root === undefined) {
        return;
    }

    try {
        if ((await lstat(file)).isSymbolicLink()) {
            throw new ValidationError(`Refusing to write through a symlink: ${file}`);
        }
    } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== 'ENOENT') {
            throw error;
        }
    }

    await assertInsideRoot(root, 'dest', dirname(file));
}
