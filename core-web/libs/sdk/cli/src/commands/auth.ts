import * as prompts from '@clack/prompts';
import { defineCommand } from 'citty';
import consola from 'consola';

import { addInstance, authenticateWithCredentials, loadAuth, removeInstance } from '../core/auth';
import { loadConfig, saveConfig } from '../core/config';

export const authCommand = defineCommand({
    meta: {
        name: 'auth',
        description: 'Manage server instances and authentication'
    },
    subCommands: {
        add: defineCommand({
            meta: { name: 'add', description: 'Add a server instance' },
            args: {
                name: { type: 'positional', description: 'Instance name', required: true },
                url: { type: 'string', description: 'Server URL', required: true },
                token: { type: 'string', description: 'API token (non-interactive)' }
            },
            async run({ args }) {
                const projectDir = process.cwd();
                const name = args.name as string;
                const url = args.url as string;
                let token = args.token as string | undefined;

                // Validate URL
                try {
                    new URL(url);
                } catch {
                    consola.error(`Invalid URL: ${url}`);
                    return;
                }

                // If no token provided, prompt for auth method
                if (!token) {
                    const authMethod = await prompts.select({
                        message: 'Authentication method',
                        options: [
                            { value: 'token', label: 'API Token' },
                            { value: 'credentials', label: 'Username & Password' },
                            { value: 'skip', label: 'Skip (configure later)' }
                        ]
                    });

                    if (prompts.isCancel(authMethod)) {
                        consola.info('Aborted.');
                        return;
                    }

                    if (authMethod === 'token') {
                        const tokenInput = await prompts.text({
                            message: 'API Token',
                            validate: (value) => {
                                if (!value) return 'Token is required';
                                return undefined;
                            }
                        });

                        if (prompts.isCancel(tokenInput)) {
                            consola.info('Aborted.');
                            return;
                        }

                        token = tokenInput;
                    } else if (authMethod === 'credentials') {
                        const username = await prompts.text({
                            message: 'Username (email)',
                            placeholder: 'admin@dotcms.com',
                            validate: (value) => {
                                if (!value) return 'Username is required';
                                return undefined;
                            }
                        });

                        if (prompts.isCancel(username)) {
                            consola.info('Aborted.');
                            return;
                        }

                        const password = await prompts.password({
                            message: 'Password',
                            validate: (value) => {
                                if (!value) return 'Password is required';
                                return undefined;
                            }
                        });

                        if (prompts.isCancel(password)) {
                            consola.info('Aborted.');
                            return;
                        }

                        try {
                            token = await authenticateWithCredentials(url, username, password);
                            consola.success('Authenticated successfully.');
                        } catch (error) {
                            consola.error('Authentication failed:', (error as Error).message);
                            return;
                        }
                    }
                }

                addInstance(projectDir, name, url, token);
                consola.success(`Instance "${name}" added (${url}).`);
            }
        }),
        list: defineCommand({
            meta: { name: 'list', description: 'List configured instances' },
            async run() {
                const projectDir = process.cwd();

                let config;
                try {
                    config = loadConfig(projectDir);
                } catch {
                    consola.error('No dotcli project found. Run `dotcli init` first.');
                    return;
                }

                const auth = loadAuth(projectDir);
                const instances = Object.entries(config.instances);

                if (instances.length === 0) {
                    consola.info('No instances configured. Run `dotcli auth add` to add one.');
                    return;
                }

                consola.info('Configured instances:\n');
                for (const [name, instance] of instances) {
                    const isDefault = name === config.default;
                    const hasAuth = !!auth[name]?.token;
                    const marker = isDefault ? ' (default)' : '';
                    const authStatus = hasAuth ? 'authenticated' : 'no auth';
                    consola.log(`  ${name}${marker} - ${instance.url} [${authStatus}]`);
                }
            }
        }),
        default: defineCommand({
            meta: { name: 'default', description: 'Set the default instance' },
            args: {
                name: { type: 'positional', description: 'Instance name', required: true }
            },
            async run({ args }) {
                const projectDir = process.cwd();
                const name = args.name as string;

                let config;
                try {
                    config = loadConfig(projectDir);
                } catch {
                    consola.error('No dotcli project found. Run `dotcli init` first.');
                    return;
                }

                if (!config.instances[name]) {
                    consola.error(
                        `Instance "${name}" not found. Available: ${Object.keys(config.instances).join(', ')}`
                    );
                    return;
                }

                config.default = name;
                saveConfig(projectDir, config);
                consola.success(`Default instance set to "${name}".`);
            }
        }),
        remove: defineCommand({
            meta: { name: 'remove', description: 'Remove a server instance' },
            args: {
                name: { type: 'positional', description: 'Instance name', required: true }
            },
            async run({ args }) {
                const projectDir = process.cwd();
                const name = args.name as string;

                try {
                    removeInstance(projectDir, name);
                    consola.success(`Instance "${name}" removed.`);
                } catch (error) {
                    consola.error((error as Error).message);
                }
            }
        })
    }
});
