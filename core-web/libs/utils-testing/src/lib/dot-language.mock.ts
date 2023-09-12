import { DotLanguage } from '@dotcms/dotcms-models';

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
