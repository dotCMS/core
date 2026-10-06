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
            // Ignore build-tool config files (e.g. the inferred rollup config
            // and the Vitest config) so their dev-only imports aren't flagged
            // as missing runtime dependencies of this types-only package.
            '@nx/dependency-checks': [
                'error',
                {
                    ignoredFiles: [
                        '{projectRoot}/rollup.config.cjs',
                        '{projectRoot}/vite.config.mts'
                    ]
                }
            ]
        },
        languageOptions: {
            parser: jsoncEslintParser
        }
    }
];
