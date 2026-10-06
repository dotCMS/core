import { FlatCompat } from '@eslint/eslintrc';
import { dirname } from 'path';
import { fileURLToPath } from 'url';
import js from '@eslint/js';
import baseConfig from '../../../eslint.config.mjs';

const compat = new FlatCompat({
    baseDirectory: dirname(fileURLToPath(import.meta.url)),
    recommendedConfig: js.configs.recommended
});

export default [
    ...baseConfig,
    ...compat.extends('plugin:@nx/react'),
    {
        files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.jsx'],
        rules: {}
    },
    {
        files: [
            '**/*.spec.ts',
            '**/*.spec.tsx',
            '**/*.test.ts',
            '**/*.test.tsx',
            '**/*.spec.js',
            '**/*.test.js'
        ],
        rules: {
            '@typescript-eslint/no-explicit-any': 'off'
        }
    }
];
