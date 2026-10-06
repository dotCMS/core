import baseConfig from '../../../eslint.config.mjs';
import nx from '@nx/eslint-plugin';

export default [
    ...baseConfig,
    ...nx.configs['flat/react'],
    {
        files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.jsx'],
        rules: {
            '@stylistic/padding-line-between-statements': [
                'error',
                {
                    blankLine: 'always',
                    prev: 'const',
                    next: '*'
                },
                {
                    blankLine: 'always',
                    prev: '*',
                    next: 'if'
                },
                {
                    blankLine: 'always',
                    prev: 'if',
                    next: '*'
                },
                {
                    blankLine: 'always',
                    prev: '*',
                    next: 'const'
                }
            ]
        }
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
    }
];
