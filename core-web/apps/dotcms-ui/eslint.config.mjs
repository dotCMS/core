import { FlatCompat } from '@eslint/eslintrc';
import { dirname } from 'path';
import { fileURLToPath } from 'url';
import js from '@eslint/js';
import baseConfig from '../../eslint.config.mjs';
import nx from '@nx/eslint-plugin';

const compat = new FlatCompat({
    baseDirectory: dirname(fileURLToPath(import.meta.url)),
    recommendedConfig: js.configs.recommended
});

export default [
    ...baseConfig,
    ...nx.configs['flat/angular'],
    {
            files: ['**/*.ts'],
            rules: {
                '@angular-eslint/directive-selector': [
                    'error',
                    {
                        type: 'attribute',
                        prefix: 'dot',
                        style: 'camelCase'
                    }
                ],
                '@angular-eslint/component-selector': [
                    'error',
                    {
                        type: 'element',
                        prefix: 'dot',
                        style: 'kebab-case'
                    }
                ],
                '@typescript-eslint/no-duplicate-enum-values': 'off',
                '@angular-eslint/prefer-standalone': 'off',
                '@angular-eslint/prefer-on-push-component-change-detection': 'off',
                '@angular-eslint/no-input-rename': 'off',
                '@angular-eslint/no-output-on-prefix': 'off',
                '@angular-eslint/no-output-native': 'off',
                '@nx/enforce-module-boundaries': [
                    'error',
                    {
                        allow: ['@dotcms/dotcms-webcomponents/loader']
                    }
                ]
            }
        },
    ...nx.configs['flat/angular-template'],
    {
        // Pre-migration parity: the repo's eslintrc setup applied only
        // @angular-eslint template/recommended, not the accessibility preset.
        // Nx's flat/angular-template adds templateAccessibility, so disable those
        // rules here to keep this migration behavior-neutral. Adopt a11y separately.
        files: ['**/*.html'],
        rules: {
            '@angular-eslint/template/alt-text': 'off',
            '@angular-eslint/template/click-events-have-key-events': 'off',
            '@angular-eslint/template/elements-content': 'off',
            '@angular-eslint/template/interactive-supports-focus': 'off',
            '@angular-eslint/template/label-has-associated-control': 'off',
            '@angular-eslint/template/mouse-events-have-key-events': 'off',
            '@angular-eslint/template/no-autofocus': 'off',
            '@angular-eslint/template/no-distracting-elements': 'off',
            '@angular-eslint/template/role-has-required-aria': 'off',
            '@angular-eslint/template/table-scope': 'off',
            '@angular-eslint/template/valid-aria': 'off'
        }
    }
];
