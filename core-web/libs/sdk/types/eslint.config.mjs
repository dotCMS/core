import baseConfig from '../../../eslint.config.mjs';
import jsoncEslintParser from 'jsonc-eslint-parser';

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
        files: ['**/*.json'],
        rules: {
            // Ignore build-tool config files (e.g. the inferred rollup config)
            // so @nx/rollup isn't flagged as a missing runtime dependency.
            '@nx/dependency-checks': [
                'error',
                {
                    ignoredFiles: ['{projectRoot}/rollup.config.cjs']
                }
            ]
        },
        languageOptions: {
            parser: jsoncEslintParser
        }
    }
];
