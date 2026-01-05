import { execa } from 'execa';
import fs from 'fs-extra';

import path from 'path';

import { downloadFile } from '../utils';

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

export async function downloadDockerCompose(directory: string) {
    // 6. Download docker-compose file
    const dockerUrl =
        'https://raw.githubusercontent.com/dotCMS/core/main/docker/docker-compose-examples/single-node-demo-site/docker-compose.yml';

    const dockerComposePath = path.join(directory, 'docker-compose.yml');

    await downloadFile(dockerUrl, dockerComposePath);
}
