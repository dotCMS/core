export default {
    input: {
        'index.esm': 'libs/sdk/analytics/src/index.ts',
        'react/index': 'libs/sdk/analytics/src/lib/react/index.ts'
    },
    output: {
        dir: 'dist/libs/sdk/analytics',
        format: 'esm',
        preserveModules: true,
        preserveModulesRoot: 'libs/sdk/analytics/src'
    }
};
