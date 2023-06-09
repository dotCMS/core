import type { StorybookConfig } from '@storybook/core-common';

const config: StorybookConfig = {
    core: { builder: 'webpack5' },
    stories: ['../**/*.stories.mdx', '../**/*.stories.@(js|jsx|ts|tsx)'],
    addons: ['@storybook/addon-essentials']
} as StorybookConfig;

module.exports = config;

// To customize your webpack configuration you can use the webpackFinal field.
// Check https://storybook.js.org/docs/react/builders/webpack#extending-storybooks-webpack-config
// and https://nx.dev/packages/storybook/documents/custom-builder-configs
