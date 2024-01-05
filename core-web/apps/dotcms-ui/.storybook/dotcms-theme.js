import { create } from '@storybook/theming/create';

export default create({
    base: 'light',

    colorPrimary: '#426BF0',
    colorSecondary: '#7042F0',

    // UI
    appBg: '#FAFAFB',
    appContentBg: '#fff',
    appBorderColor: '#6C7389',
    appBorderRadius: 4,

    // Typography
    fontBase: 'Assistant, "Helvetica Neue", Helvetica, Arial, "Lucida Grande", sans-serif',
    fontCode: 'Menlo, Monaco, Consolas',

    // Text colors
    textColor: '#14151A',
    textInverseColor: '#fff',

    // Toolbar default and active colors
    barTextColor: '#fff',
    barSelectedColor: '#fff',
    barBg: '#426BF0',

    // Form colors
    inputBg: '#fff',
    inputBorder: '#6C7389',
    inputTextColor: '#14151A',
    inputBorderRadius: 2,

    brandTitle: 'DotCMS Components Library',
    brandUrl: 'https://dotcms.com',
    brandImage: 'https://dotcms.com/dA/99fe3769-d649/256w/dotcms.png'
});
