import baseConfig from '../../eslint.config.mjs';
import nx from '@nx/eslint-plugin';

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
                    style: 'camelCase'
                }
            ],
            '@angular-eslint/component-selector': [
                'error',
                {
                    type: 'element',
                    style: 'kebab-case'
                }
            ],
            '@angular-eslint/prefer-standalone': 'off',
            '@angular-eslint/prefer-on-push-component-change-detection': 'off',
            '@angular-eslint/no-input-rename': 'off'
        }
    },
    {
        /**
         * Test doubles, held to different rules than product code — a PRE-EXISTING
         * break, not something this migration introduced. `utils-testing:lint` had 36
         * errors on main; nothing had noticed because the project rarely lands in
         * `nx affected`, and it only surfaced here because this PR adds shared mocks to
         * it.
         *
         * The three rules are wrong for this directory by construction:
         *   - component-selector: a mock component has to answer to the SAME selector as
         *     the component it replaces, so a prefix requirement cannot be met.
         *   - no-empty-function: an empty body IS the stub.
         *   - no-unused-vars: a stub keeps the real signature so call sites typecheck.
         */
        files: ['src/lib/**/*.mock.ts', 'src/lib/test-setup-helpers.ts'],
        rules: {
            '@angular-eslint/component-selector': 'off',
            '@typescript-eslint/no-empty-function': 'off',
            '@typescript-eslint/no-unused-vars': 'off'
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
