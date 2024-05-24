import { DotLanguage, DotLanguagesISO } from '@dotcms/dotcms-models';

export const mockDotLanguage: DotLanguage = {
    id: 1,
    languageCode: 'en',
    countryCode: 'US',
    language: 'English',
    country: 'United States'
};

export const mockDotLanguageWithoutCountryCode: DotLanguage = {
    id: 2,
    languageCode: 'IT',
    countryCode: '',
    language: 'Italian',
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
        defaultLanguage: true
    },
    {
        id: 2,
        languageCode: 'es',
        countryCode: 'ES',
        language: 'Spanish',
        country: 'Spain',
        isoCode: 'es-ES',
        defaultLanguage: false
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
