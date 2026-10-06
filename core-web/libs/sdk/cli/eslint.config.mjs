import baseConfig from '../../../eslint.config.mjs';

/**
 * The dependency direction that keeps this package legible as it grows.
 *
 * `dotcms agent setup` is the first command of a CLI that will absorb `create-app` and the
 * dotCLI port as sibling groups. The tree is organised by command group rather than by
 * technical layer so a new group is a new directory; this rule is what stops that from
 * eroding the first time someone needs "just one thing" from a sibling.
 *
 *   commands/<group>/**  ->  shared/**       allowed
 *   shared/**            ->  commands/**     forbidden
 *   commands/a/**        ->  commands/b/**   forbidden
 */
export default [
    ...baseConfig,
    {
        files: ['libs/sdk/cli/src/shared/**/*.ts'],
        rules: {
            'no-restricted-imports': [
                'error',
                {
                    patterns: [
                        {
                            group: ['**/commands/*'],
                            message:
                                'shared/ must not import from a command group — it would couple the shared layer to one command and break the "a group is a leaf" rule.'
                        }
                    ]
                }
            ]
        }
    },
    {
        files: ['libs/sdk/cli/src/commands/agent/**/*.ts'],
        rules: {
            'no-restricted-imports': [
                'error',
                {
                    patterns: [
                        {
                            group: ['**/commands/!(agent)/**'],
                            message:
                                'Command groups must not import each other. Promote the shared piece into shared/ instead.'
                        }
                    ]
                }
            ]
        }
    }
];
