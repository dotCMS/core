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
                    ignoredDependencies: [
                        'rxjs',
                        '@angular/core',
                        '@angular/forms',
                        'vite',
                        'primeng',
                        '@nx/vite',
                        'vite-tsconfig-paths'
                    ]
                }
            ]
        },
        languageOptions: {
            parser: jsoncEslintParser
        }
    }
];
