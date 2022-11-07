/* eslint-disable */
export default {
    displayName: 'dotcms-models',
    preset: '../../jest.preset.js',
    globals: {
        'ts-jest': {
            tsconfig: '<rootDir>/tsconfig.spec.json'
        }
    },
    transform: {
        '^.+\\.[tj]s$': 'ts-jest'
    },
    moduleFileExtensions: ['ts', 'js', 'html'],
    coverageDirectory: '../../coverage/libs/dotcms-models'
};
