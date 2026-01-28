import type { FrameworkChoices, SupportedFrontEndFrameworks } from '../types';

export const DOTCMS_HOST = 'http://localhost:8082';
export const DOTCMS_USER = {
    username: 'admin@dotcms.com',
    password: 'admin'
};

// USED APIS
export const DOTCMS_HEALTH_API = `${DOTCMS_HOST}/api/v1/probes/alive`;
export const DOTCMS_TOKEN_API = `${DOTCMS_HOST}/api/v1/authentication/api-token`;
export const DOTCMS_EMA_CONFIG_API = `${DOTCMS_HOST}/api/v1/apps/dotema-config-v2/`;
export const DOTCMS_DEMO_SITE = `${DOTCMS_HOST}/api/v1/site/`;

// App constants
export const FRAMEWORKS: SupportedFrontEndFrameworks[] = [
    'nextjs',
    'astro',
    'angular',
    'angular-ssr'
];

export const FRAMEWORKS_CHOICES: FrameworkChoices[] = [
    { name: 'Next.js', value: 'nextjs' },
    { name: 'Astro', value: 'astro' },
    { name: 'Angular', value: 'angular' },
    { name: 'Angular (SSR)', value: 'angular-ssr' }
];

export const NEXTJS_DEPENDENCIES: string[] = [
    '@dotcms/client',
    '@dotcms/experiments',
    '@dotcms/react',
    '@dotcms/types',
    '@dotcms/uve',
    '@tinymce/tinymce-react',
    'next',
    'react',
    'react-dom'
];

export const NEXTJS_DEPENDENCIES_DEV: string[] = [
    '@tailwindcss/postcss',
    '@tailwindcss/typography',
    'eslint',
    'eslint-config-next',
    'postcss',
    'prettier',
    'tailwindcss'
];

export const ASTRO_DEPENDENCIES: string[] = [
    '@astrojs/check',
    '@astrojs/react',
    '@astrojs/ts-plugin',
    '@astrojs/vercel',
    '@dotcms/client',
    '@dotcms/react',
    '@dotcms/types',
    '@dotcms/uve',
    '@tailwindcss/typography',
    '@tailwindcss/vite',
    '@tinymce/tinymce-react',
    '@types/react',
    '@types/react-dom',
    'astro',
    'dotenv',
    'react',
    'react-dom',
    'tailwindcss',
    'typescript'
];

export const ASTRO_DEPENDENCIES_DEV: string[] = [
    '@types/node',
    'prettier',
    'prettier-plugin-astro'
];

export const ANGULAR_DEPENDENCIES: string[] = [
    '@angular/animations',
    '@angular/common',
    '@angular/compiler',
    '@angular/core',
    '@angular/forms',
    '@angular/platform-browser',
    '@angular/platform-browser-dynamic',
    '@angular/router',
    '@dotcms/angular',
    '@dotcms/client',
    '@dotcms/types',
    '@dotcms/uve',
    'rxjs',
    'tslib',
    'zone.js'
];

export const ANGULAR_DEPENDENCIES_DEV: string[] = [
    '@angular/build',
    '@angular/cli',
    '@angular/compiler-cli',
    '@tailwindcss/typography',
    '@types/jasmine',
    'autoprefixer',
    'jasmine-core',
    'karma',
    'karma-chrome-launcher',
    'karma-coverage',
    'karma-jasmine',
    'karma-jasmine-html-reporter',
    'postcss',
    'tailwindcss',
    'typescript'
];

export const ANGULAR_SSR_DEPENDENCIES: string[] = [
    '@angular/common',
    '@angular/compiler',
    '@angular/core',
    '@angular/forms',
    '@angular/platform-browser',
    '@angular/platform-server',
    '@angular/router',
    '@angular/ssr',
    '@dotcms/angular',
    '@dotcms/client',
    '@dotcms/types',
    '@dotcms/uve',
    '@tailwindcss/postcss',
    '@types/dotenv',
    'dotenv',
    'express',
    'postcss',
    'rxjs',
    'tailwindcss',
    'tslib',
    'zone.js'
];

export const ANGULAR_SSR_DEPENDENCIES_DEV: string[] = [
    '@angular/build',
    '@angular/cli',
    '@angular/compiler-cli',
    '@tailwindcss/typography',
    '@types/express',
    '@types/jasmine',
    '@types/node',
    'jasmine-core',
    'karma',
    'karma-chrome-launcher',
    'karma-coverage',
    'karma-jasmine',
    'karma-jasmine-html-reporter',
    'typescript'
];
