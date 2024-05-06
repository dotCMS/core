module.exports = {
    addons: [
        {
            name: '@storybook/addon-docs',
            options: {
                configureJSX: true,
                babelOptions: {},
                sourceLoaderOptions: null,
                transcludeMarkdown: true
            }
        },
        '@storybook/addon-actions'
    ],

    // uncomment the property below if you want to apply some webpack config globally
    // webpackFinal: async (config, { configType }) => {
    //   // Make whatever fine-grained changes you need that should apply to all storybook configs
    //   // Return the altered config
    //   return config;
    // },
    stories: [],

    framework: {
        name: '@storybook/angular',
        options: {
            builder: {
                useSWC: true
            }
        }
    },

    docs: {
        autodocs: true
    },

    swc: (config, options) => {
        return {
            ...config
        };
    }
};
