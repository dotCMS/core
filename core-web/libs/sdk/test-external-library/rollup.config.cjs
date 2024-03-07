// const resolve = require('@rollup/plugin-node-resolve');
// const commonjs = require('@rollup/plugin-commonjs');
const terser = require('@rollup/plugin-terser');

module.exports = (config) => {
    return {
        ...config,
        output: {
            dir: 'dist/libs/sdk/test-external-library',
            // format: 'umd',
            format: 'iife',
            name: 'MyCustomLibrary'
        },
        // plugins: [resolve(), commonjs(), ...nxConfig.plugins],
        plugins: [terser(), ...config.plugins]
    };
};
