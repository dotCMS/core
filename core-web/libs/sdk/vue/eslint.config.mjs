import skipFormatting from '@vue/eslint-config-prettier/skip-formatting';
import vue from 'eslint-plugin-vue';

import baseConfig from '../../../eslint.config.mjs';

export default [
    ...baseConfig,
    ...vue.configs['flat/recommended'],
    {
        files: ['**/*.vue'],
        languageOptions: {
            parserOptions: {
                parser: await import('@typescript-eslint/parser')
            }
        }
    },
    {
        files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.jsx', '**/*.vue'],
        rules: {
            'vue/multi-word-component-names': 'off'
        }
    },
    // Must stay LAST. `vue.configs['flat/recommended']` ships formatting rules
    // (`vue/html-indent` at 2 spaces, `vue/max-attributes-per-line`,
    // `vue/html-self-closing`) that contradict `.prettierrc` (`tabWidth: 4`).
    // They are only `warn`, so `nx lint` stayed green while `eslint --fix`
    // silently rewrote every `.vue` file into a shape Prettier then rejects.
    // This turns those rules off and leaves `nx format` the sole formatter.
    skipFormatting
];
