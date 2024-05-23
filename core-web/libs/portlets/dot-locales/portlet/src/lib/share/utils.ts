import { DotLanguage } from '@dotcms/dotcms-models';

export const getLocaleISOCode = (locale: DotLanguage) =>
    locale ? `${locale.languageCode}${locale.countryCode ? `-${locale.countryCode}` : ''}` : '';
