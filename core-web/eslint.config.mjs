import { FlatCompat } from '@eslint/eslintrc';
import { dirname } from 'path';
import { fileURLToPath } from 'url';
import js from '@eslint/js';
import nx from '@nx/eslint-plugin';
import eslintPluginImport from 'eslint-plugin-import';
import eslintPluginBan from 'eslint-plugin-ban';
import stylisticEslintPlugin from '@stylistic/eslint-plugin';
import eslintPluginBetterTailwindcss from 'eslint-plugin-better-tailwindcss';
import angularEslintTemplateParser from '@angular-eslint/template-parser';

const compat = new FlatCompat({
    baseDirectory: dirname(fileURLToPath(import.meta.url)),
    recommendedConfig: js.configs.recommended
});

export default [
    ...nx.configs['flat/base'],
    {
        plugins: {
            import: eslintPluginImport,
            ban: eslintPluginBan,
            '@stylistic': stylisticEslintPlugin,
            'better-tailwindcss': eslintPluginBetterTailwindcss
        }
    },
    {
        settings: {
            'better-tailwindcss': {
                entryPoint: 'apps/dotcms-ui/src/style.css'
            }
        }
    },
    {
        files: ['**/*.ts', '**/*.tsx', '**/*.js', '**/*.jsx'],
        rules: {
            '@nx/enforce-module-boundaries': [
                'error',
                {
                    allowCircularSelfDependency: false,
                    enforceBuildableLibDependency: true,
                    allow: [],
                    depConstraints: [
                        {
                            sourceTag: '*',
                            onlyDependOnLibsWithTags: ['*']
                        }
                    ]
                }
            ]
        }
    },
    ...nx.configs['flat/typescript'],
    {
        files: ['**/*.ts', '**/*.tsx'],
        rules: {
            '@nx/dependency-checks': [
                'error',
                {
                    ignoredFiles: ['{projectRoot}/rollup.config.{js,ts,mjs,mts}']
                }
            ],
            '@typescript-eslint/no-explicit-any': ['error'],
            '@typescript-eslint/no-unused-vars': [
                'error',
                {
                    argsIgnorePattern: '^_'
                }
            ],
            '@typescript-eslint/no-unused-expressions': [
                'error',
                {
                    allowTernary: true
                }
            ],
            'one-var': [
                'error',
                {
                    var: 'never',
                    let: 'never',
                    const: 'never'
                }
            ],
            'no-console': [
                'error',
                {
                    allow: ['warn', 'error']
                }
            ],
            'no-duplicate-imports': 'error',
            'import/no-cycle': [
                'error',
                {
                    maxDepth: 10,
                    ignoreExternal: true
                }
            ],
            'import/no-self-import': 'error',
            'ban/ban': [
                2,
                {
                    name: ['describe', 'only'],
                    message: "don't focus tests"
                },
                {
                    name: 'fdescribe',
                    message: "don't focus tests"
                },
                {
                    name: ['it', 'only'],
                    message: "don't focus tests"
                },
                {
                    name: 'fit',
                    message: "don't focus tests"
                },
                {
                    name: ['test', 'only'],
                    message: "don't focus tests"
                },
                {
                    name: 'ftest',
                    message: "don't focus tests"
                }
            ],
            'import/order': [
                'error',
                {
                    'newlines-between': 'always',
                    groups: [
                        'external',
                        'builtin',
                        'internal',
                        'object',
                        'index',
                        'sibling',
                        'parent',
                        'type'
                    ],
                    pathGroups: [
                        {
                            pattern: '@angular/**',
                            group: 'external',
                            position: 'after'
                        },
                        {
                            pattern: 'primeng/**',
                            group: 'external',
                            position: 'after'
                        },
                        {
                            pattern: 'rxjs/**',
                            group: 'external',
                            position: 'after'
                        },
                        {
                            pattern: '@tiptap/**',
                            group: 'external',
                            position: 'after'
                        },
                        {
                            pattern: '@components/**',
                            group: 'internal'
                        },
                        {
                            pattern: '@directives/**',
                            group: 'internal'
                        },
                        {
                            pattern: '@dotcms-ui/**',
                            group: 'internal'
                        },
                        {
                            pattern: '@dotcms/**',
                            group: 'internal'
                        },
                        {
                            pattern: '@portlets/**',
                            group: 'internal'
                        },
                        {
                            pattern: '@models/**',
                            group: 'internal'
                        },
                        {
                            pattern: '@pipes/**',
                            group: 'internal'
                        },
                        {
                            pattern: '@services/**',
                            group: 'internal'
                        },
                        {
                            pattern: '@shared/**',
                            group: 'internal'
                        },
                        {
                            pattern: '@tests/**',
                            group: 'internal'
                        }
                    ],
                    pathGroupsExcludedImportTypes: ['internal'],
                    alphabetize: {
                        order: 'asc',
                        caseInsensitive: true
                    }
                }
            ]
        }
    },
    {
        files: ['**/*.ts', '**/*.tsx'],
        rules: {
            '@stylistic/keyword-spacing': [
                'error',
                {
                    after: true
                }
            ]
        }
    },
    ...nx.configs['flat/javascript'],
    ...compat
        .config({
            env: {
                jest: true
            }
        })
        .map((config) => ({
            ...config,
            files: ['**/*.spec.ts', '**/*.spec.tsx', '**/*.spec.js', '**/*.spec.jsx'],
            rules: {
                ...config.rules,
                '@typescript-eslint/no-non-null-assertion': 'off'
            }
        })),
    {
        files: ['**/*.html'],
        rules: {
            'better-tailwindcss/enforce-consistent-variable-syntax': [
                'error',
                {
                    syntax: 'shorthand'
                }
            ],
            'better-tailwindcss/enforce-consistent-important-position': [
                'error',
                {
                    position: 'recommended'
                }
            ],
            'better-tailwindcss/no-duplicate-classes': 'error',
            'better-tailwindcss/no-unnecessary-whitespace': 'error',
            'better-tailwindcss/no-deprecated-classes': 'error',
            'better-tailwindcss/enforce-canonical-classes': 'error',
            'better-tailwindcss/no-unknown-classes': 'off',
            'better-tailwindcss/no-conflicting-classes': 'warn',
            'better-tailwindcss/enforce-consistent-class-order': 'off',
            'better-tailwindcss/enforce-consistent-line-wrapping': 'off'
        },
        languageOptions: {
            parser: angularEslintTemplateParser
        }
    },
    {
        ignores: [
            '**/*.md',
            '/**/node_modules/*',
            '**/*vite.config*.timestamp*',
            '**/*vitest.config*.timestamp*',
            // Generated / build output — never linted (mirrors .gitignore)
            '**/.xmcp/**',
            '**/src/generated/**',
            '**/dist/**'
        ]
    }
];
