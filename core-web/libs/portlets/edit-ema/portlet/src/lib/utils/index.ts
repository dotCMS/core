import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';

import { DotPageApiParams } from '../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../shared/consts';
import { ActionPayload, ContainerPayload, PageContainer } from '../shared/models';

export const SDK_EDITOR_SCRIPT_SOURCE = '/html/js/editor-js/sdk-editor.js';

/**
 * Insert a contentlet in a container
 *
 * @export
 * @param {ActionPayload} action
 * @return {*}  {{
 *    pageContainers: PageContainer[];
 *   didInsert: boolean;
 * }}
 */
export function insertContentletInContainer(action: ActionPayload): {
    pageContainers: PageContainer[];
    didInsert: boolean;
} {
    if (action.position) {
        return insertPositionedContentletInContainer(action);
    }

    let didInsert = false;

    const { pageContainers, container, personaTag, newContentletId } = action;

    const newPageContainers = pageContainers.map((pageContainer) => {
        if (
            areContainersEquals(pageContainer, container) &&
            !pageContainer.contentletsId.includes(newContentletId)
        ) {
            pageContainer.contentletsId.push(newContentletId);
            didInsert = true;
        }

        pageContainer.personaTag = personaTag;

        return pageContainer;
    });

    return {
        pageContainers: newPageContainers,
        didInsert
    };
}

/**
 * Delete a contentlet from a container
 *
 * @export
 * @param {ActionPayload} action
 * @return {*}  {PageContainer[]}
 */
export function deleteContentletFromContainer(action: ActionPayload): {
    pageContainers: PageContainer[];
    contentletsId: string[];
} {
    const { pageContainers, container, contentlet, personaTag } = action;

    let contentletsId = [];

    const newPageContainers = pageContainers.map((currentContainer) => {
        if (areContainersEquals(currentContainer, container)) {
            const newContentletsId = currentContainer.contentletsId.filter(
                (id) => id !== contentlet.identifier
            );

            contentletsId = newContentletsId;

            return {
                ...currentContainer,
                contentletsId: newContentletsId,
                personaTag
            };
        }

        return currentContainer;
    });

    return {
        pageContainers: newPageContainers,
        contentletsId
    };
}

/**
 * Check if two containers are equals
 *
 * @param {PageContainer} currentContainer
 * @param {ContainerPayload} containerToFind
 * @return {*}  {boolean}
 */
export function areContainersEquals(
    currentContainer: PageContainer,
    containerToFind?: ContainerPayload
): boolean {
    return (
        currentContainer.identifier === containerToFind?.identifier &&
        currentContainer.uuid === containerToFind?.uuid
    );
}

/**
 * Insert a contentlet in a container in a specific position
 *
 * @export
 * @param {ActionPayload} payload
 * @return {*}  {{
 *    pageContainers: PageContainer[];
 *   didInsert: boolean;
 * }}
 */
function insertPositionedContentletInContainer(payload: ActionPayload): {
    pageContainers: PageContainer[];
    didInsert: boolean;
} {
    let didInsert = false;

    const { pageContainers, container, contentlet, personaTag, newContentletId, position } =
        payload;

    const newPageContainers = pageContainers.map((pageContainer) => {
        if (
            areContainersEquals(pageContainer, container) &&
            !pageContainer.contentletsId.includes(newContentletId)
        ) {
            const index = pageContainer.contentletsId.indexOf(contentlet.identifier);

            if (index !== -1) {
                const offset = position === 'before' ? index : index + 1;
                pageContainer.contentletsId.splice(offset, 0, newContentletId);

                didInsert = true;
            } else {
                pageContainer.contentletsId.push(newContentletId);
                didInsert = true;
            }
        }

        pageContainer.personaTag = personaTag;

        return pageContainer;
    });

    return {
        pageContainers: newPageContainers,
        didInsert
    };
}

/**
 * Remove the index from the end of the url if it's nested and also remove extra slashes
 *
 * @param {string} url
 * @return {*}  {string}
 */
export function sanitizeURL(url?: string): string {
    return url
        ?.replace(/(^\/)|(\/$)/g, '') // Remove slashes from the beginning and end of the url
        .replace(/\/index$/, ''); // Remove index from the end of the url
}

/**
 * Get the personalization for the contentlet
 *
 * @param {Record<string, string>} persona
 * @return {*}
 */
export const getPersonalization = (persona: Record<string, string>) => {
    if (!persona || (!persona.contentType && !persona.keyTag)) {
        return `dot:default`;
    }

    return `dot:${persona.contentType}:${persona.keyTag}`;
};

/**
 * Create a page api url with query params
 *
 * @export
 * @param {string} url
 * @param {Partial<DotPageApiParams>} params
 * @return {*}  {string}
 */
export function createPageApiUrlWithQueryParams(
    url: string,
    params: Partial<DotPageApiParams>
): string {
    // Set default values
    const completedParams = {
        ...params,
        language_id: params.language_id ?? '1',
        'com.dotmarketing.persona.id':
            params['com.dotmarketing.persona.id'] ?? DEFAULT_PERSONA.identifier,
        variantName: params.variantName ?? DEFAULT_VARIANT_ID
    };

    // Filter out undefined values and url
    Object.keys(completedParams).forEach(
        (key) =>
            (completedParams[key] === undefined || key === 'url') && delete completedParams[key]
    );

    const queryParams = new URLSearchParams({
        ...completedParams
    }).toString();

    return queryParams.length ? `${url}?${queryParams}` : url;
}

/**
 * Check if the variant is the default one
 *
 * @export
 * @param {string} [variant]
 * @return {*}  {boolean}
 */
export function getIsDefaultVariant(variant?: string): boolean {
    return !variant || variant === DEFAULT_VARIANT_ID;
}
