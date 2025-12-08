import type { FramworkChoices, SupprotedFrontEndFramworks } from '../types';

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
export const FRAMEWORKS: SupprotedFrontEndFramworks[] = [
    'nextjs',
    'astro',
    'vuejs',
    'angular',
    'angular-ssr'
];

export const FRAMEWORKS_CHOICES: FramworkChoices[] = [
    { name: 'Next.js', value: 'nextjs' },
    { name: 'Astro', value: 'astro' },
    { name: 'Vue', value: 'vuejs' },
    { name: 'Angular', value: 'angular' },
    { name: 'Angular (SSR)', value: 'angular-ssr' }
];
