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
                                '../generated/*'
                            ],
                            message:
                                '@dotcms/ai/sandbox is the generic engine and must not import dotCMS-specific code (adapter, spec, runtime). Keep the generic/dotCMS boundary intact.'
                        }
                    ]
                }
            ]
        },
        ignores: ['src/sandbox/**/*.spec.ts']
    },
    {
        ignores: ['**/node_modules/**', 'node_modules/**']
    }
];
