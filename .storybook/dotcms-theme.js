import { create } from '@storybook/theming/create';

export default create({
    base: 'light',

    colorPrimary: '#6B4DE2',
    colorSecondary: '#6B4DE2',

    // UI
    appBg: '#ede8fb',
    appContentBg: '#fff',
    appBorderColor: '#93929f',
    appBorderRadius: 4,

    // Typography
    fontBase: 'Roboto, "Helvetica Neue", Helvetica, Arial, "Lucida Grande", sans-serif',
    fontCode: 'Menlo, Monaco, Consolas',

    // Text colors
    textColor: '#0a0725',
    textInverseColor: '#fff',

    // Toolbar default and active colors
    barTextColor: '#fff',
    barSelectedColor: '#fff',
    barBg: '#6B4DE2',

    // Form colors
    inputBg: '#fff',
    inputBorder: '#93929f',
    inputTextColor: '#0a0725',
    inputBorderRadius: 2,

    brandTitle: 'DotCMS Components Library',
    brandUrl: 'https://dotcms.com',
    brandImage:
        'https://dotcms.com/dA/99fe3769-d649/256w/dotcms.png'
});
