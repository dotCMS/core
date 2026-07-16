import { FlatCompat } from '@eslint/eslintrc';
import { dirname } from 'path';
import { fileURLToPath } from 'url';
import js from '@eslint/js';
import baseConfig from '../../eslint.config.mjs';

const compat = new FlatCompat({
    baseDirectory: dirname(fileURLToPath(import.meta.url)),
    recommendedConfig: js.configs.recommended
});

export default [
    ...baseConfig,
    ...compat.extends('plugin:playwright/recommended'),
    {
        rules: {
            'playwright/expect-expect': 'off',
            'playwright/no-wait-for-timeout': 'error',
            'playwright/no-force-option': 'error',
            'playwright/no-skipped-test': 'error',
            'playwright/no-conditional-in-test': 'error',
            'playwright/prefer-locator': 'error'
        }
    },
    {
        files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.jsx'],
        rules: {
            '@nx/enforce-module-boundaries': 'off'
        }
    },
    ...compat
        .config({
            extends: ['plugin:@typescript-eslint/strict', 'plugin:@typescript-eslint/stylistic']
        })
        .map((config) => ({
            ...config,
            files: ['**/*.ts', '**/*.tsx'],
            rules: {
                ...config.rules,
                '@typescript-eslint/no-unused-vars': [
                    'error',
                    {
                        argsIgnorePattern: '^_'
                    }
                ]
            }
        })),
    {
        files: ['**/*.js', '**/*.jsx'],
        // Override or add rules here
        rules: {}
    },
    {
        ignores: ['playwright-report/**', 'test-results/**', 'target/**']
    }
];
