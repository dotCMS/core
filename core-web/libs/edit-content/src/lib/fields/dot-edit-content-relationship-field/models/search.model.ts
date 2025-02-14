import { DotLanguage } from '@dotcms/dotcms-models';

/**
 * Interface representing the parameters for a search operation.
 *
 * @export
 * @interface SearchParams
 */
export interface SearchParams {
    languageId: DotLanguage['id'];
    siteId: string;
    query: string;
}
