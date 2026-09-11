import fs from 'fs-extra';

import os from 'node:os';
import path from 'node:path';

const COMPOSE_FILE = 'docker-compose.yml';

/**
 * Runs `action` with `docker-compose.yml` moved out of `directory`, then puts it back.
 *
 * The move is required because git clones into an empty directory. The `finally` is required
 * because without it a scaffolding failure strands the compose file outside the project —
 * leaving the user unable to `docker compose down` the stack that is still running. The failure
 * destroyed the recovery path (AC-008).
 *
 * The holding spot is a private temp directory, NOT the parent. The parent is the user's cwd:
 * moving there with `overwrite` silently destroyed a `docker-compose.yml` the user already had,
 * and the `finally` then moved our file into the project, so the original was unrecoverable.
 * A fresh `mkdtemp` per call also means concurrent runs cannot collide.
 *
 * The original error is deliberately allowed to propagate: a `finally` that swallowed it would
 * replace one silent failure with another.
 */
export async function withComposeFileMovedAside<T>(
    directory: string,
    action: () => Promise<T>
): Promise<T> {
    const inProject = path.join(directory, COMPOSE_FILE);
    const moved = fs.existsSync(inProject);

    if (!moved) {
        return await action();
    }

    const holdingDir = await fs.mkdtemp(path.join(os.tmpdir(), 'dotcms-create-app-compose-'));
    const asideNext = path.join(holdingDir, COMPOSE_FILE);

    await fs.move(inProject, asideNext);

    try {
        return await action();
    } finally {
        if (fs.existsSync(asideNext)) {
            await fs.move(asideNext, inProject, { overwrite: true });
        }
        await fs.remove(holdingDir);
    }
}
