/* eslint-disable */
export default {
    displayName: 'sdk-analytics',
    preset: '../../../jest.preset.js',
    transform: {
        '^(?!.*\\.(js|jsx|ts|tsx|css|json)$)': '@nx/react/plugins/jest',
        '^.+\\.[tj]sx?$': [
            'babel-jest',
            {
                presets: ['@nx/react/babel'],
                plugins: [
                    ['@babel/plugin-proposal-private-methods', { loose: true }],
                    ['@babel/plugin-proposal-private-property-in-object', { loose: true }],
                    ['@babel/plugin-proposal-class-properties', { loose: true }]
                ]
            }
        ]
    },
    moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx'],
    coverageDirectory: '../../../coverage/libs/sdk/analytics'
};
