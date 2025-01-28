import { DotLanguage } from '@dotcms/dotcms-models';

export interface SearchParams {
    languageId: DotLanguage['id'];
    siteId: string;
    query: string;
}

