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
        // Inside @dotcms/ai/tools the layers only point down: definitions → operations →
        // toolkit. The toolkit is how ANY tool is built and knows no dotCMS operation; see
        // src/tools/README.md for what goes where.
        files: ['src/tools/toolkit/**/*.ts'],
        rules: {
            'no-restricted-imports': [
                'error',
                {
                    patterns: [
                        {
                            group: [
                                '**/operations',
                                '**/operations/*',
                                '**/definitions',
                                '**/definitions/*'
                            ],
                            message:
                                'tools/toolkit is how any tool is built; it must not depend on a specific operation or tool definition. Move dotCMS-specific code into tools/operations.'
                        }
                    ]
                }
            ]
        }
    },
    {
        // The consumer owns configuration: credentials arrive through the dotCMS connection,
        // never from ambient environment variables the tools go looking for.
        files: ['src/tools/**/*.ts'],
        ignores: ['src/tools/**/*.spec.ts'],
        rules: {
            'no-restricted-properties': [
                'error',
                {
                    object: 'process',
                    property: 'env',
                    message:
                        'The tools never read the environment. Configuration comes from the consumer, through dotcmsConnection({ url, token }).'
                }
            ]
        }
    },
    {
        files: ['src/tools/operations/**/*.ts'],
        rules: {
            'no-restricted-imports': [
                'error',
                {
                    patterns: [
                        {
                            group: ['**/definitions', '**/definitions/*'],
                            message:
                                'tools/operations is the direct layer and must work without the model-facing tools. Definitions import operations, never the reverse.'
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
