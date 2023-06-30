const path = require('path');
const rootMain = require('../../../.storybook/main');

module.exports = {
    ...rootMain,

    core: { ...rootMain.core, builder: 'webpack5' },

    stories: [
        ...rootMain.stories,
        '../src/**/*.stories.mdx',
        '../src/**/*.stories.@(js|jsx|ts|tsx)',
        '../../../libs/template-builder/**/*.stories.@(js|jsx|ts|tsx|mdx)',
        '../../../libs/block-editor/**/*.stories.@(js|jsx|ts|tsx|mdx)'
    ],
    addons: ['storybook-design-token', '@storybook/addon-essentials', ...rootMain.addons],
    features: {
        previewMdx2: true
    },
    webpackFinal: async (config, { configType }) => {
        // apply any global webpack configs that might have been specified in .storybook/main.js
        if (rootMain.webpackFinal) {
            config = await rootMain.webpackFinal(config, { configType });
        }

        config.module.rules.push({
            test: /\.css$/,
            use: ['style-loader', 'css-loader'],
            include: path.resolve(__dirname, '../../../libs/dotcms-scss/shared')
            // include: path.resolve(__dirname, '../'),
        });

        return config;
    }
};
