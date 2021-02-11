module.exports = {
    stories: ['../src/**/*.stories.mdx', '../src/**/*.stories.@(js|jsx|ts|tsx)'],
    addons: ['@storybook/addon-links', '@storybook/addon-essentials'],
    refs: {
        dotcmsWebComponents: {
            title: 'Dotcms Web Componentes',
            url: 'https://dotcms.github.io/dotcms-webcomponents/'
        }
    }
};
