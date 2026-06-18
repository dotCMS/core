const path = require('path');
const webpack = require('webpack');
const rootMain = require('../../../.storybook/main');

module.exports = {
    ...rootMain,

    core: { ...rootMain.core },

    stories: [
        ...rootMain.stories,
        '../src/**/*.stories.mdx',
        '../src/**/*.stories.@(js|jsx|ts|tsx)',
        '../../../libs/template-builder/**/*.stories.@(js|jsx|ts|tsx|mdx)',
        '../../../libs/block-editor/**/*.stories.@(js|jsx|ts|tsx|mdx)',
        '../../../libs/edit-content/**/*.stories.@(js|jsx|ts|tsx|mdx)',
        '../../../libs/ui/**/*.stories.@(js|jsx|ts|tsx|mdx)',
        '../../../libs/portlets/**/*.stories.@(js|jsx|ts|tsx|mdx)'
    ],
    addons: ['@storybook/addon-essentials', ...rootMain.addons],
    webpackFinal: async (config, { configType }) => {
        // apply any global webpack configs that might have been specified in .storybook/main.js
        if (rootMain.webpackFinal) {
            config = await rootMain.webpackFinal(config, { configType });
        }

        config.module.rules.push({
            test: /\.css$/,
            use: ['style-loader', 'css-loader'],
            include: path.resolve(__dirname, '../../../libs/dotcms-scss/shared')
        });

        // add USE_MIDDLEWARE environment variable to the storybook build
        config.plugins.push(
            new webpack.DefinePlugin({
                'process.env.USE_MIDDLEWARE': JSON.stringify(process.env.USE_MIDDLEWARE)
            })
        );

        return config;
    },
    middleware: './middleware.js'
};
