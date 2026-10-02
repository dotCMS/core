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
            '@angular-eslint/prefer-standalone': 'off',
            '@angular-eslint/prefer-on-push-component-change-detection': 'off',
            '@angular-eslint/no-input-rename': 'off',
            '@angular-eslint/no-output-on-prefix': 'off',
            '@angular-eslint/no-output-native': 'off'
        }
    },
    ...nx.configs['flat/angular-template'],
    {
        // templateAccessibility is enabled here (#36386). The whole preset runs as
        // Nx ships it; the one option below is not a relaxation.
        //
        // `elements-content` looks for text content, aria-label, aria-labelledby or
        // title on <button>/<a>/<h1-6>. It cannot see content a component projects,
        // and PrimeNG's `pButton` directive takes `label` as an input and renders it
        // as the button's text. So `<button pButton label="Cancel">` reports as empty
        // when it is not. `label` is added to the allowList to describe that, rather
        // than adding an aria-label that duplicates text already on screen.
        //
        // Buttons that genuinely had no accessible name were fixed instead: `ariaLabel`
        // is NOT an input on PrimeNG 21's ButtonDirective (`label`, `icon`, `severity`
        // and friends are), so `ariaLabel="Delete Action"` was reaching the DOM as an
        // inert `arialabel` attribute and those icon-only buttons were announced with
        // no name at all. They now use the real `aria-label`.
        files: ['**/*.html'],
        rules: {
            '@angular-eslint/template/elements-content': ['error', { allowList: ['label'] }]
        }
    }
];
