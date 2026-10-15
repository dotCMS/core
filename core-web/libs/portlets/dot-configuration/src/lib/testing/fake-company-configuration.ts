import {
    DotCompanyAuthType,
    DotCompanyConfiguration,
    DotLoginLanguage
} from '@dotcms/dotcms-models';

/** Company configuration as the API returns it, with test-specific overrides. */
export const createFakeCompanyConfiguration = (
    overrides: Partial<DotCompanyConfiguration> = {}
): DotCompanyConfiguration => ({
    companyId: 'dotcms.org',
    companyName: 'dotcms.org',
    portalURL: 'localhost',
    emailAddress: 'dotCMS Website <website@dotcms.com>',
    mx: 'dotcms.com',
    primaryColor: '#4e65f1',
    secondaryColor: '#233f9b',
    backgroundColor: '#1b3359',
    backgroundImage: '/dA/background-id/asset/bg.jpg',
    loginScreenLogo: '/dA/logo-id/asset/logo.svg',
    navBarLogo: null,
    authType: DotCompanyAuthType.EMAIL_ADDRESS,
    keyDigest: 'a2ec430f79ac24c7cda029000cc6addb45c28762ae22ea18f5',
    languageId: 'en_US',
    timeZoneId: 'UTC',
    ...overrides
});

export const FAKE_ADMIN_LOCALES: DotLoginLanguage[] = [
    { language: 'en', country: 'US', displayName: 'English (United States)' },
    { language: 'es', country: 'ES', displayName: 'español (España)' }
];
