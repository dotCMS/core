import { execa } from 'execa';
import fs from 'fs-extra';

import path from 'path';

import { resolveComposeSource } from '../compose/compose-source';

import type { SupportedFrontEndFrameworks } from '../types';

export const cloneFrontEndSample = async ({
    framework,
    directory
}: {
    framework: SupportedFrontEndFrameworks;
    directory: string;
}) => {
    // 1. Clone repository (sparse + blobless)
    await execa(
        'git',
        ['clone', '--filter=blob:none', '--sparse', 'https://github.com/dotCMS/core.git', '.'],
        { cwd: directory }
    );

    // 2. Enable sparse checkout & select the folder
    await execa('git', ['sparse-checkout', 'set', `examples/${framework}`], {
        cwd: directory
    });

    // await fs.ensureDir(path.join(directory, framework));

    // 3. Checkout main branch (only the selected folder is downloaded)
    await execa('git', ['checkout', 'main'], {
        cwd: directory
        // stdio: 'inherit'
    });

    const src = path.join(directory, 'examples', `${framework}`);
    const dest = directory;
    // const dest = path.join(directory, framework);

    // Ensure framework directory exists
    // await fs.ensureDir(dest);

    // Remove EVERYTHING in repo except the examples folder
    const items = await fs.readdir(directory);

    for (const item of items) {
        if (item !== 'examples') {
            await fs.remove(path.join(directory, item));
        }
    }

    // Copy only the nextjs folder into the framework folder
    await fs.copy(src, dest, { overwrite: true });

    // Remove the remaining examples folder
    const allItems = await fs.readdir(directory);

    for (const item of allItems) {
        if (item === 'examples') {
            await fs.remove(path.join(directory, item));
        }
    }
};

/**
 * Writes the docker-compose file into the project directory.
 *
 * The file is **bundled with this package** rather than fetched from `main` at run
 * time. The previous behaviour downloaded
 * `docker/docker-compose-examples/single-node-demo-site/docker-compose.yml`, which
 * meant every installed CLI silently picked up whatever was on `main` — so the
 * shared example could not be hardened without shipping that change, unversioned,
 * to consumers who never asked for it. Owning the file removes that coupling; the
 * shared example is now left untouched (issue #37262, AC-010).
 *
 * `DOTCMS_COMPOSE_URL` keeps remote fetching one env var away for field hotfixes.
 */
export async function downloadDockerCompose(directory: string) {
    const source = resolveComposeSource();
    const dockerComposePath = path.join(directory, 'docker-compose.yml');

    const contents = await source.read();
    await fs.writeFile(dockerComposePath, contents);
}

export async function moveDockerComposeOneLevelUp(directory: string) {
    const sourcePath = path.join(directory, 'docker-compose.yml');
    const targetPath = path.join(directory, '..', 'docker-compose.yml');
    await fs.rename(sourcePath, targetPath);
}

export async function moveDockerComposeBack(directory: string) {
    const sourcePath = path.join(directory, '..', 'docker-compose.yml');
    const targetPath = path.join(directory, 'docker-compose.yml');

    await fs.rename(sourcePath, targetPath);
}
