import { DotLanguage } from '@dotcms/dotcms-models';

export const getLocaleISOCode = (locale: DotLanguage): string => {
    if (locale) {
        return `${locale.languageCode}${locale.countryCode ? `-${locale.countryCode}` : ''}`;
    }

    return '';
};
