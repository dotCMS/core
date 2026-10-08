import baseConfig from '../../../eslint.config.mjs';

// The package's layers, enforced here instead of only described in CLAUDE.md. A file matches one
// block below: flat config lets the last block that sets a rule replace the earlier ones.
const SPECS = ['**/*.spec.ts', '**/*.spec.tsx'];
const NO_REACT = {
    paths: [
        { name: 'react', message: 'React stays in src/lib/react, behind the ./react entry.' },
        { name: 'react-dom', message: 'React stays in src/lib/react, behind the ./react entry.' }
    ],
    group: ['react/*', 'react-dom/*']
};
// The capabilities built on the pipeline, which the pipeline never imports
const CAPABILITIES = [
    '**/contentlets/**',
    '**/impressions/**',
    '**/clicks/**',
    '**/experiments/**',
    '**/react/**'
];
const PIPELINE_MESSAGE =
    'pipeline/ is what every event goes through: it imports nothing from the capabilities or the events layer.';
const restrict = (patterns, allowReact = false) => [
    'error',
    {
        paths: allowReact ? [] : NO_REACT.paths,
        patterns: [
            ...patterns,
            ...(allowReact
                ? []
                : [
                      {
                          group: NO_REACT.group,
                          message: 'React stays in src/lib/react, behind the ./react entry.'
                      }
                  ])
        ]
    }
];

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
            // verbatimModuleSyntax erases an import only when it says `import type`, so a module
            // that provides both values and types is imported twice, on purpose.
            'no-duplicate-imports': ['error', { allowSeparateTypeImports: true }],
            '@typescript-eslint/consistent-type-imports': [
                'error',
                { prefer: 'type-imports', fixStyle: 'separate-type-imports' }
            ]
        }
    },
    {
        files: ['**/*.js', '**/*.jsx'],
        // Override or add rules here
        rules: {}
    },
    {
        // The pipeline every event goes through: it knows nothing about the capabilities built
        // on it. Files sit one or two folders below src/lib, and `../models` is the public types
        // from the first and the pipeline's own from the second, hence two blocks.
        files: ['src/lib/pipeline/*.ts'],
        ignores: SPECS,
        rules: {
            'no-restricted-imports': restrict([
                { group: [...CAPABILITIES, '../events', '../models'], message: PIPELINE_MESSAGE }
            ])
        }
    },
    {
        files: ['src/lib/pipeline/*/*.ts'],
        ignores: SPECS,
        rules: {
            'no-restricted-imports': restrict([
                {
                    group: [...CAPABILITIES, '../../events', '../../models'],
                    message: PIPELINE_MESSAGE
                }
            ])
        }
    },
    {
        // What the content trackers share: it builds on the pipeline only
        files: ['src/lib/contentlets/**/*.ts'],
        ignores: SPECS,
        rules: {
            'no-restricted-imports': restrict([
                {
                    group: [
                        '**/impressions/**',
                        '**/clicks/**',
                        '**/experiments/**',
                        '**/react/**',
                        '../events',
                        '../models'
                    ],
                    message: 'contentlets/ builds on pipeline/ only: the trackers build on it.'
                }
            ])
        }
    },
    {
        files: ['src/lib/impressions/**/*.ts'],
        ignores: SPECS,
        rules: {
            'no-restricted-imports': restrict([
                {
                    group: [
                        '**/clicks/**',
                        '**/experiments/**',
                        '**/react/**',
                        '../events',
                        '../models'
                    ],
                    message: 'impressions/ builds on pipeline/ and contentlets/ only.'
                }
            ])
        }
    },
    {
        files: ['src/lib/clicks/**/*.ts'],
        ignores: SPECS,
        rules: {
            'no-restricted-imports': restrict([
                {
                    group: [
                        '**/impressions/**',
                        '**/experiments/**',
                        '**/react/**',
                        '../events',
                        '../models'
                    ],
                    message: 'clicks/ builds on pipeline/ and contentlets/ only.'
                }
            ])
        }
    },
    {
        files: ['src/lib/experiments/**/*.ts'],
        ignores: SPECS,
        rules: {
            'no-restricted-imports': restrict([
                {
                    // A regex, because a gitignore negation cannot re-include a file whose
                    // folder an earlier pattern already excluded
                    regex: '^(?!.*/contentlets/constants$).*/contentlets/',
                    message: 'experiments/ takes only the rescan event from contentlets/.'
                },
                {
                    group: ['**/impressions/**', '**/clicks/**', '**/react/**', '../events'],
                    message:
                        'experiments/ builds on pipeline/: the events layer and the React adapter build on it.'
                }
            ])
        }
    },
    {
        // The adapter brings the markup, never the engine: that loads with dotEvents.init
        files: ['src/lib/react/**/*.ts', 'src/lib/react/**/*.tsx'],
        ignores: SPECS,
        rules: {
            'no-restricted-imports': restrict(
                [
                    {
                        group: [
                            '**/pipeline/**',
                            '**/contentlets/**',
                            '**/impressions/**',
                            '**/clicks/**',
                            '../experiments/engine',
                            '../experiments/api',
                            '../experiments/store',
                            '../experiments/plugin',
                            '../events'
                        ],
                        message:
                            'The React adapter imports the markup only: no engine, pipeline or trackers.'
                    }
                ],
                true
            )
        }
    },
    {
        // The script tag config of the IIFE: it reads attributes and knows the public types only
        files: ['src/lib/standalone/**/*.ts'],
        ignores: SPECS,
        rules: {
            'no-restricted-imports': restrict([
                {
                    group: [...CAPABILITIES, '**/pipeline/**', '../events'],
                    message: 'standalone/ reads the script tag: it imports the public types only.'
                }
            ])
        }
    },
    {
        // The IIFE dotCMS injects: the events object and the script tag config, nothing else
        files: ['src/standalone.ts'],
        rules: {
            'no-restricted-imports': restrict([
                {
                    group: [
                        './lib/pipeline/**',
                        './lib/contentlets/**',
                        './lib/impressions/**',
                        './lib/clicks/**',
                        './lib/experiments/**',
                        './lib/react/**'
                    ],
                    message:
                        'The standalone script starts events from its script tag: it imports ./lib/events and ./lib/standalone only.'
                }
            ])
        }
    },
    {
        // Every page loads the root entry: it must not reference the boot script builder
        files: ['src/index.ts'],
        rules: {
            'no-restricted-imports': restrict([
                {
                    group: [
                        './lib/experiments/markup',
                        './lib/experiments/boot',
                        './lib/react/**',
                        './lib/standalone/**'
                    ],
                    message:
                        'The root entry does not re-export the markup: some bundlers keep a re-exported module in the chunk every page loads.'
                }
            ])
        }
    },
    {
        files: ['src/markup.ts'],
        rules: {
            'no-restricted-imports': restrict([
                {
                    group: [
                        './lib/events',
                        './lib/experiments/engine',
                        './lib/pipeline/**',
                        './lib/contentlets/**',
                        './lib/impressions/**',
                        './lib/clicks/**',
                        './lib/react/**'
                    ],
                    message: './markup is framework-free and holds no engine.'
                }
            ])
        }
    },
    {
        files: ['src/lib/*.ts'],
        ignores: SPECS,
        rules: {
            'no-restricted-imports': restrict([
                {
                    group: ['./react/**'],
                    message: 'React stays in src/lib/react, behind the ./react entry.'
                }
            ])
        }
    }
];
