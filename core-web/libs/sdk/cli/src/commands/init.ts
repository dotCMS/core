import * as prompts from '@clack/prompts';
import { defineCommand } from 'citty';
import consola from 'consola';

import * as fs from 'node:fs';
import * as path from 'node:path';

import { addInstance, authenticateWithCredentials, saveAuth } from '../core/auth';
import { saveConfig } from '../core/config';
import { AUTH_FILE, CACHE_DIR, DOTCLI_DIR, type DotCliConfig } from '../core/types';

const DEFAULT_DOTCLIIGNORE = [
    '# Ignore binary asset caches',
    '**/assets/',
    '',
    '# Ignore system content',
    '*/content/SystemContent/',
    '',
    '# Ignore temp files',
    '*.tmp',
    '.DS_Store',
    ''
].join('\n');

const GITIGNORE_ENTRIES = [
    `.dotcli/${AUTH_FILE}`,
    `.dotcli/${CACHE_DIR}/`,
    `.dotcli/snapshot.json`
];

export const initCommand = defineCommand({
    meta: {
        name: 'init',
        description: 'Initialize a dotcli project in the current directory'
    },
    async run() {
        const projectDir = process.cwd();
        const dotcliDir = path.join(projectDir, DOTCLI_DIR);

        if (fs.existsSync(dotcliDir)) {
            consola.warn('A .dotcli directory already exists in this project.');
            const overwrite = await prompts.confirm({
                message: 'Reinitialize the project?'
            });

            if (prompts.isCancel(overwrite) || !overwrite) {
                consola.info('Aborted.');
                return;
            }
        }

        prompts.intro('dotcli init');

        // Create directory structure
        fs.mkdirSync(path.join(dotcliDir, CACHE_DIR), { recursive: true });

        // Create .dotcliignore if it doesn't exist
        const ignorePath = path.join(projectDir, '.dotcliignore');
        if (!fs.existsSync(ignorePath)) {
            fs.writeFileSync(ignorePath, DEFAULT_DOTCLIIGNORE, 'utf-8');
        }

        // Append to .gitignore
        appendToGitignore(projectDir);

        // Interactive setup
        const serverUrl = await prompts.text({
            message: 'Server URL',
            placeholder: 'https://demo.dotcms.com',
            validate: (value) => {
                if (!value) return 'URL is required';
                try {
                    new URL(value);
                    return undefined;
                } catch {
                    return 'Invalid URL';
                }
            }
        });

        if (prompts.isCancel(serverUrl)) {
            // Save empty config even if canceled
            const config: DotCliConfig = { default: '', instances: {} };
            saveConfig(projectDir, config);
            saveAuth(projectDir, {});
            consola.info('Initialized .dotcli with empty config.');
            return;
        }

        const instanceName = await prompts.text({
            message: 'Instance name',
            placeholder: 'demo',
            initialValue: new URL(serverUrl).hostname.split('.')[0],
            validate: (value) => {
                if (!value) return 'Name is required';
                if (!/^[a-zA-Z0-9_-]+$/.test(value))
                    return 'Only alphanumeric, dash, and underscore allowed';
                return undefined;
            }
        });

        if (prompts.isCancel(instanceName)) {
            const config: DotCliConfig = { default: '', instances: {} };
            saveConfig(projectDir, config);
            saveAuth(projectDir, {});
            consola.info('Initialized .dotcli with empty config.');
            return;
        }

        const authMethod = await prompts.select({
            message: 'Authentication method',
            options: [
                { value: 'token', label: 'API Token' },
                { value: 'credentials', label: 'Username & Password' },
                { value: 'skip', label: 'Skip (configure later)' }
            ]
        });

        if (prompts.isCancel(authMethod)) {
            const config: DotCliConfig = { default: '', instances: {} };
            saveConfig(projectDir, config);
            saveAuth(projectDir, {});
            consola.info('Initialized .dotcli with empty config.');
            return;
        }

        let token: string | undefined;

        if (authMethod === 'token') {
            const tokenInput = await prompts.text({
                message: 'API Token',
                validate: (value) => {
                    if (!value) return 'Token is required';
                    return undefined;
                }
            });

            if (!prompts.isCancel(tokenInput)) {
                token = tokenInput;
            }
        } else if (authMethod === 'credentials') {
            const username = await prompts.text({
                message: 'Username (email)',
                placeholder: 'admin@dotcms.com',
                validate: (value) => {
                    if (!value) return 'Username is required';
                    return undefined;
                }
            });

            if (!prompts.isCancel(username)) {
                const password = await prompts.password({
                    message: 'Password',
                    validate: (value) => {
                        if (!value) return 'Password is required';
                        return undefined;
                    }
                });

                if (!prompts.isCancel(password)) {
                    try {
                        const spinner = prompts.spinner();
                        spinner.start('Authenticating...');
                        token = await authenticateWithCredentials(serverUrl, username, password);
                        spinner.stop('Authenticated successfully.');
                    } catch (error) {
                        consola.error('Authentication failed:', (error as Error).message);
                    }
                }
            }
        }

        // Save config and auth
        addInstance(projectDir, instanceName, serverUrl, token);

        prompts.outro('Project initialized! Run `dotcli content pull` to get started.');
    }
});

function appendToGitignore(projectDir: string): void {
    const gitignorePath = path.join(projectDir, '.gitignore');
    let existing = '';

    if (fs.existsSync(gitignorePath)) {
        existing = fs.readFileSync(gitignorePath, 'utf-8');
    }

    const linesToAdd = GITIGNORE_ENTRIES.filter((entry) => !existing.includes(entry));

    if (linesToAdd.length > 0) {
        const block = '\n# dotcli\n' + linesToAdd.join('\n') + '\n';
        fs.appendFileSync(gitignorePath, block, 'utf-8');
    }
}
