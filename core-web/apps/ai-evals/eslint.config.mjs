import baseConfig from '../../eslint.config.mjs';

export default [
    ...baseConfig,
    {
        // ai-evals is a Node CLI eval harness — console output to stdout is
        // intentional (it is the program's primary output), so no-console is off.
        files: ['**/*.ts'],
        rules: {
            'no-console': 'off'
        }
    }
];
