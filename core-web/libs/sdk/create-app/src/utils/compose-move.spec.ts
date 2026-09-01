/**
 * Contract spec for the compose-file round trip around scaffolding
 * (task T033, dotCMS #37262, AC-008).
 *
 * THE BUG. `docker-compose.yml` is moved one level UP before scaffolding, because git requires
 * an empty directory to clone into, then moved BACK afterwards. If scaffolding fails in between,
 * the move-back never runs and the compose file is stranded in the PARENT directory — so the
 * user cannot even `docker compose down` the stack that is still running. The recovery path is
 * destroyed by the failure it is meant to survive, which is the shape of this whole issue.
 *
 * The fix is `try/finally`: the file comes back whether scaffolding succeeds or throws, and the
 * original error still propagates — a `finally` that swallows the cause would trade one silent
 * failure for another.
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
});
