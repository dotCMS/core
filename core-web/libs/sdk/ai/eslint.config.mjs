import baseConfig from '../../../eslint.config.mjs';

export default [
    ...baseConfig,
    {
        files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.jsx'],
        // Override or add rules here
        rules: {}
    },
    {
        files: ['**/*.ts', '**/*.tsx'],
        // Override or add rules here
        rules: {}
    },
    {
        files: ['**/*.js', '**/*.jsx'],
        // Override or add rules here
        rules: {}
    },
    {
        files: ['src/sandbox/**/*.ts'],
        rules: {
            'no-restricted-imports': [
                'error',
                {
                    patterns: [
                        {
                            group: [
                                '../adapter',
                                '../adapter/*',
                                '../spec',
                                '../spec/*',
                                '../runtime',
                                '../tools',
                                '../tools/*',
                                '../generated/*'
                            ],
                            message:
                                '@dotcms/ai/sandbox is the generic engine and must not import dotCMS-specific code (adapter, spec, runtime, tools). Keep the generic/dotCMS boundary intact.'
                        }
                    ]
                }
            ]
        },
        ignores: ['src/sandbox/**/*.spec.ts']
    },
    {
        // `@dotcms/ai/tools` is the top layer: it is built ON the runtime, so nothing beneath
        // it may reach back up. Otherwise a bare `@dotcms/ai/runtime` import would start
        // dragging in the tool descriptions, the asset-transfer code and `node:fs`.
        files: ['src/runtime.ts', 'src/adapter/**/*.ts', 'src/spec/**/*.ts'],
        rules: {
            'no-restricted-imports': [
                'error',
                {
                    patterns: [
                        {
                            group: ['./tools', './tools/*', '../tools', '../tools/*'],
                            message:
                                '@dotcms/ai/tools is the top layer and builds on the runtime; the runtime, adapter and spec must not import it.'
                        }
                    ]
                }
            ]
        }
    },
    {
        ignores: ['**/node_modules/**', 'node_modules/**']
    }
];
