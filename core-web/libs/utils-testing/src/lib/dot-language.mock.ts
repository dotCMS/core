import { DotLanguage, DotLanguagesISO } from '@dotcms/dotcms-models';

export const mockDotLanguage: DotLanguage = {
    id: 1,
    languageCode: 'en',
    countryCode: 'US',
    language: 'English',
    country: 'United States',
    translated: true
};

export const mockDotLanguageWithoutCountryCode: DotLanguage = {
    id: 2,
    languageCode: 'IT',
    countryCode: '',
    language: 'Italian',
    translated: false,
    country: 'Italy'
};

export const mockLanguageArray: DotLanguage[] = [
    mockDotLanguage,
    mockDotLanguageWithoutCountryCode
];

export const mockLocales: DotLanguage[] = [
    {
        id: 1,
        languageCode: 'en',
        countryCode: 'US',
        language: 'English',
        country: 'United States',
        isoCode: 'en-US',
        defaultLanguage: true,
        variables: { count: 1, total: 5 }
    },
    {
        id: 2,
        languageCode: 'es',
        countryCode: 'ES',
        language: 'Spanish',
        country: 'Spain',
        isoCode: 'es-ES',
        defaultLanguage: false,
        variables: { count: 1, total: 1 }
    }
];

export const mockLanguagesISO: DotLanguagesISO = {
    countries: [
        { code: 'US', name: 'United States' },
        { code: 'CA', name: 'Canada' }
    ],
    languages: [
        { code: 'en', name: 'English' },
        { code: 'es', name: 'Spanish' }
    ]
};

/**
 * Creates a fake language with optional overrides.
 * @param overrides - Partial overrides for the default language properties.
 * @returns {DotLanguage} - The fake language with applied overrides.
 */
export function createFakeLanguage(overrides: Partial<DotLanguage> = {}): DotLanguage {
    return { ...mockDotLanguage, ...overrides };
}
