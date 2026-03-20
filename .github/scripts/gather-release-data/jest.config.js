/** @type {import('ts-jest').JestConfigWithTsJest} */
module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  roots: ['<rootDir>/src'],
  testMatch: ['**/*.test.ts'],
  // @octokit packages are ESM-only; transform them with ts-jest
  transformIgnorePatterns: [
    'node_modules/(?!(@octokit|universal-user-agent|before-after-hook)/)',
  ],
  transform: {
    '^.+\\.tsx?$': 'ts-jest',
    '^.+\\.jsx?$': ['ts-jest', { tsconfig: { allowJs: true } }],
  },
};
