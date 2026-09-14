import { dirname, join } from 'node:path';
import { existsSync } from 'node:fs';

/**
 * Attaches a `typecheck` target to every project that ships specs.
 *
 * Specs are the one surface nothing else covers. Library sources are type checked
 * transitively when a consuming app builds them, but `nx test` runs on ts-jest with
 * `isolatedModules`, which transpiles without ever building a program — so a spec can carry
 * type errors indefinitely while its suite stays green.
 *
 * Inferring the target rather than declaring it in ~30 `project.json` files is deliberate:
 * the gap this closes was itself created by a project being missed, and a per-project
 * declaration would let the next one slip the same way.
 */
const TARGET = 'typecheck';

export const createNodesV2 = [
    '**/tsconfig.spec.json',
    async (configFiles) => {
        return configFiles.map((configFile) => {
            const projectRoot = dirname(configFile);

            // Only claim real projects, and never override a target another plugin already
            // infers — the Vite projects get their own `typecheck` from @nx/vite/plugin.
            if (!existsSync(join(projectRoot, 'project.json'))) return [configFile, {}];
            const hasViteConfig = ['ts', 'mts', 'js', 'mjs'].some((ext) =>
                existsSync(join(projectRoot, `vite.config.${ext}`))
            );
            if (hasViteConfig) return [configFile, {}];

            return [
                configFile,
                {
                    projects: {
                        [projectRoot]: {
                            targets: {
                                [TARGET]: {
                                    command: 'tsc -p tsconfig.spec.json --noEmit',
                                    options: { cwd: projectRoot },
                                    cache: true,
                                    inputs: [
                                        'default',
                                        '^default',
                                        '{workspaceRoot}/tsconfig.base.json',
                                        '{projectRoot}/tsconfig.json',
                                        '{projectRoot}/tsconfig.spec.json'
                                    ],
                                    metadata: {
                                        description:
                                            'Type checks this project’s specs, which `nx test` cannot (ts-jest transpiles only).'
                                    }
                                }
                            }
                        }
                    }
                }
            ];
        });
    }
];
