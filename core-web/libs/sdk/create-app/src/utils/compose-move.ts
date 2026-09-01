import fs from 'fs-extra';

import path from 'node:path';

const COMPOSE_FILE = 'docker-compose.yml';

/**
 * Runs `action` with `docker-compose.yml` moved out of `directory`, then puts it back.
 *
 * The move is required because git clones into an empty directory. The `finally` is required
 * because without it a scaffolding failure strands the compose file in the PARENT directory —
 * leaving the user unable to `docker compose down` the stack that is still running. The failure
 * destroyed the recovery path (AC-008).
 *
 * The original error is deliberately allowed to propagate: a `finally` that swallowed it would
 * replace one silent failure with another.
 */
export async function withComposeFileMovedAside<T>(
    directory: string,
    action: () => Promise<T>
): Promise<T> {
    const inProject = path.join(directory, COMPOSE_FILE);
    const asideNext = path.join(directory, '..', COMPOSE_FILE);
    const moved = fs.existsSync(inProject);

    if (moved) {
        await fs.move(inProject, asideNext, { overwrite: true });
    }

    try {
        return await action();
    } finally {
        if (moved && fs.existsSync(asideNext)) {
            await fs.move(asideNext, inProject, { overwrite: true });
        }
    }
}
