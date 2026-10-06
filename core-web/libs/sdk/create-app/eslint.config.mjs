import baseConfig from '../../../eslint.config.mjs';
import jsoncEslintParser from 'jsonc-eslint-parser';

export default [
    ...baseConfig,
    {
        files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.jsx'],
        rules: {
            'no-console': 'off'
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
    },
    {
        files: ['**/*.json'],
        // Override or add rules here
        rules: {},
        languageOptions: {
            parser: jsoncEslintParser
        }
    }
];
