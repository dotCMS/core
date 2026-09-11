/**
 * Contract spec for the compose-file round trip around scaffolding
 * (task T033, dotCMS #37262, AC-008).
 *
 * THE BUG. `docker-compose.yml` is moved OUT of the project before scaffolding, because git
 * requires an empty directory to clone into, then moved BACK afterwards. If scaffolding fails in
 * between, the move-back never runs and the compose file is stranded outside the project — so
 * the user cannot even `docker compose down` the stack that is still running. The recovery path
 * is destroyed by the failure it is meant to survive, which is the shape of this whole issue.
 *
 * The fix is `try/finally`: the file comes back whether scaffolding succeeds or throws, and the
 * original error still propagates — a `finally` that swallows the cause would trade one silent
 * failure for another.
 *
 * TWO CONSTRAINTS THIS SPEC EXISTS TO HOLD, both regressions found in review:
 *
 *   1. The holding spot is a private temp dir, never the parent. The parent is the user's cwd,
 *      and moving there with `overwrite` destroyed a compose file they already had.
 *   2. `action` must signal failure by THROWING. `process.exit` skips `finally`, so a caller
 *      that exits instead of throwing silently reintroduces the stranded-file bug — which is
 *      exactly what `startScaffoldingFrontEnd` did (see src/index.ts).
 *
 * API PINNED
 *   export async function withComposeFileMovedAside<T>(
 *       directory: string,
 *       action: () => Promise<T>
 *   ): Promise<T>;
 */

import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import { withComposeFileMovedAside } from './compose-move';

const COMPOSE = 'docker-compose.yml';
const CONTENTS = 'services:\n  dotcms:\n    image: dotcms/dotcms:latest\n';

describe('withComposeFileMovedAside', () => {
    let parentDir: string;
    let projectDir: string;

    beforeEach(() => {
        parentDir = fs.mkdtempSync(path.join(os.tmpdir(), 'dotcms-compose-move-'));
        projectDir = path.join(parentDir, 'my-app');
        fs.mkdirSync(projectDir);
        fs.writeFileSync(path.join(projectDir, COMPOSE), CONTENTS, 'utf8');
    });

    afterEach(() => {
        fs.rmSync(parentDir, { recursive: true, force: true });
    });

    const inProject = () => fs.existsSync(path.join(projectDir, COMPOSE));
    const inParent = () => fs.existsSync(path.join(parentDir, COMPOSE));

    it('moves the file aside for the action and restores it on success', async () => {
        let sawEmptyProjectDir = false;

        const result = await withComposeFileMovedAside(projectDir, async () => {
            // git needs the directory empty — this is the whole reason for the dance.
            sawEmptyProjectDir = !inProject();

            return 'scaffolded';
        });

        expect(sawEmptyProjectDir).toBe(true);
        expect(result).toBe('scaffolded');
        expect(inProject()).toBe(true);
        expect(inParent()).toBe(false);
    });

    it('restores the file when the action THROWS — the bug being fixed', async () => {
        await expect(
            withComposeFileMovedAside(projectDir, async () => {
                throw new Error('scaffolding failed');
            })
        ).rejects.toThrow('scaffolding failed');

        expect(inProject()).toBe(true);
    });

    it('leaves no orphan in the parent directory after a failure', async () => {
        await expect(
            withComposeFileMovedAside(projectDir, async () => {
                throw new Error('scaffolding failed');
            })
        ).rejects.toThrow();

        // The stranded copy is what made the old failure unrecoverable: `docker compose down`
        // needs this file to be where the user is standing.
        expect(inParent()).toBe(false);
    });

    it('propagates the original error rather than swallowing it in the finally', async () => {
        const cause = new Error('framework template not found');

        await expect(
            withComposeFileMovedAside(projectDir, async () => {
                throw cause;
            })
        ).rejects.toBe(cause);
    });

    it('does nothing surprising when there is no compose file to move', async () => {
        fs.rmSync(path.join(projectDir, COMPOSE));

        const result = await withComposeFileMovedAside(projectDir, async () => 'ok');

        expect(result).toBe('ok');
        expect(inParent()).toBe(false);
    });

    it('preserves the file contents across the round trip', async () => {
        await withComposeFileMovedAside(projectDir, async () => undefined);

        expect(fs.readFileSync(path.join(projectDir, COMPOSE), 'utf8')).toBe(CONTENTS);
    });

    // The parent directory is the user's cwd. The first implementation moved our compose file
    // there with `overwrite: true`, so a `docker-compose.yml` the user already had was silently
    // destroyed — and the `finally` then moved OUR file into the project, leaving no copy of
    // theirs anywhere. Scaffolding into a directory that already runs its own compose stack is
    // an ordinary thing to do, so this is data loss on a normal path.
    it('does not touch a docker-compose.yml the user already has in the parent directory', async () => {
        const theirs = 'services:\n  their-own-app:\n    image: nginx\n';
        fs.writeFileSync(path.join(parentDir, COMPOSE), theirs, 'utf8');

        await withComposeFileMovedAside(projectDir, async () => 'scaffolded');

        expect(fs.readFileSync(path.join(parentDir, COMPOSE), 'utf8')).toBe(theirs);
        expect(fs.readFileSync(path.join(projectDir, COMPOSE), 'utf8')).toBe(CONTENTS);
    });

    it('leaves the parent compose file alone even when the action fails', async () => {
        const theirs = 'services:\n  their-own-app:\n    image: nginx\n';
        fs.writeFileSync(path.join(parentDir, COMPOSE), theirs, 'utf8');

        await expect(
            withComposeFileMovedAside(projectDir, async () => {
                throw new Error('scaffolding failed');
            })
        ).rejects.toThrow('scaffolding failed');

        expect(fs.readFileSync(path.join(parentDir, COMPOSE), 'utf8')).toBe(theirs);
        expect(fs.readFileSync(path.join(projectDir, COMPOSE), 'utf8')).toBe(CONTENTS);
    });
});
