import { PageApiOptions } from '../../client/sdk-js-client';

/**
 * Interface representing the properties for page request parameters.
 *
 * @export
 * @interface PageRequestParamsProps
 */
export interface PageRequestParamsProps {
    /**
     * The API endpoint path.
     * @type {string}
     */
    path: string;

    /**
     * The query parameters for the API request.
     * Can be an object with key-value pairs or a URLSearchParams instance.
     * @type {{ [key: string]: unknown } | URLSearchParams}
     */
    params: { [key: string]: unknown } | URLSearchParams;
}

/**
 * Generates the page request parameters to be used in the API call.
 *
 * @param {PageRequestParamsProps} PageRequestParamsProps - The properties for the page request.
 * @returns {PageApiOptions} The options for the page API.
 * @example
 * ```ts
 * const pageApiOptions = getPageRequestParams({ path: '/api/v1/page', params: queryParams });
 * ```
 */
export const getPageRequestParams = ({
    path = '',
    params = {}
}: PageRequestParamsProps): PageApiOptions => {
    const copiedParams: PageRequestParamsProps['params'] =
        params instanceof URLSearchParams ? Object.fromEntries(params.entries()) : { ...params };

    const finalParams: Record<string, unknown> = {};
    const dotMarketingPersonaId = copiedParams['com.dotmarketing.persona.id'] || '';

    if (copiedParams['mode']) {
        finalParams['mode'] = copiedParams['mode'];
    }

    if (copiedParams['language_id']) {
        finalParams['language_id'] = copiedParams['language_id'];
    }

    if (copiedParams['variantName']) {
        finalParams['variantName'] = copiedParams['variantName'];
    }

    if (copiedParams['personaId'] || dotMarketingPersonaId) {
        finalParams['personaId'] = copiedParams['personaId'] || dotMarketingPersonaId;
    }

    return {
        path,
        ...finalParams
    };
};
