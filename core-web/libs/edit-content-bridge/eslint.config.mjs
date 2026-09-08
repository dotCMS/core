import baseConfig from '../../eslint.config.mjs';
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
        rules: {
            '@nx/enforce-module-boundaries': [
                'error',
                {
                    allow: ['@dotcms/ui']
                }
            ]
        }
    },
    {
        files: ['**/*.js', '**/*.jsx'],
        // Override or add rules here
        rules: {}
    },
    {
        files: ['**/*.json'],
        rules: {
            '@nx/dependency-checks': [
                'error',
                {
                    // ONE options object — eslint rejects more than one for a rule.
                    // `@nx/vite` is here because vite.config.mts imports nxViteTsPaths
                    // for the test run; it is a dev-only import and must not become a
                    // runtime dependency of this published library, which is what
                    // `eslint --fix` would otherwise write into package.json.
                    ignoredFiles: ['{projectRoot}/vite.config.mts'],
                    ignoredDependencies: [
                        'rxjs',
                        '@angular/core',
                        '@angular/forms',
                        'vite',
                        'primeng',
                        'vite-tsconfig-paths',
                        '@analogjs/vite-plugin-angular',
                        '@nx/vite'
                    ]
                }
            ]
        },
        languageOptions: {
            parser: jsoncEslintParser
        }
    }
];
