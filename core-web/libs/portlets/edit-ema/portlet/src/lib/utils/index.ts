import { CurrentUser } from '@dotcms/dotcms-js';
import {
    DEFAULT_VARIANT_ID,
    DotContainerMap,
    DotExperiment,
    DotExperimentStatus,
    DotPageContainerStructure,
    VanityUrl
} from '@dotcms/dotcms-models';

import { EmaDragItem } from '../edit-ema-editor/components/ema-page-dropzone/types';
import { DotPageApiParams } from '../services/dot-page-api.service';
import { COMMON_ERRORS, DEFAULT_PERSONA } from '../shared/consts';
import { EDITOR_STATE } from '../shared/enums';
import {
    ActionPayload,
    ContainerPayload,
    ContentletDragPayload,
    ContentTypeDragPayload,
    DotPage,
    DragDatasetItem,
    PageContainer
} from '../shared/models';

export const SDK_EDITOR_SCRIPT_SOURCE = '/html/js/editor-js/sdk-editor.js';

export const TEMPORAL_DRAG_ITEM: EmaDragItem = {
    baseType: 'dotAsset',
    contentType: 'dotAsset',
    draggedPayload: {
        type: 'temp'
    }
};

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
        language_id: params?.language_id ?? '1',
        'com.dotmarketing.persona.id':
            params?.['com.dotmarketing.persona.id'] ?? DEFAULT_PERSONA.identifier,
        variantName: params?.variantName ?? DEFAULT_VARIANT_ID
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

/**
 * Check if the param is a forward or page
 *
 * @export
 * @param {VanityUrl} vanityUrl
 * @return {*}
 */
export function isForwardOrPage(vanityUrl?: VanityUrl): boolean {
    return !vanityUrl || (!vanityUrl.permanentRedirect && !vanityUrl.temporaryRedirect);
}

/**
 * Create the url to add a page to favorites
 *
 * @private
 * @param {{
 *         languageId: number;
 *         pageURI: string;
 *         deviceInode?: string;
 *         siteId?: string;
 *     }} params
 * @return {*}  {string}
 * @memberof EditEmaStore
 */
export function createFavoritePagesURL(params: {
    languageId: number;
    pageURI: string;
    siteId: string;
}): string {
    const { languageId, pageURI, siteId } = params;

    return (
        `/${pageURI}?` +
        (siteId ? `host_id=${siteId}` : '') +
        `&language_id=${languageId}`
    ).replace(/\/\//g, '/');
}

/**
 *
 * @description Create a full URL with the clientHost
 * @export
 * @param {DotPageApiParams} params
 * @return {*}  {string}
 */
/**
 * @description Create a full URL with the clientHost
 * @export
 * @param {DotPageApiParams} params
 * @param {string} [siteId]
 * @return {*}  {string}
 */
export function createFullURL(params: DotPageApiParams, siteId?: string): string {
    // If we are going to delete properties from the params, we need to make a copy of it
    const paramsCopy = { ...params };

    if (siteId) {
        paramsCopy['host_id'] = siteId;
    }

    const clientHost = paramsCopy?.clientHost ?? window.location.origin;
    const url = paramsCopy?.url;

    // Clean the params that are not needed for the page
    delete paramsCopy?.clientHost;
    delete paramsCopy?.url;
    delete paramsCopy?.mode;

    const searchParams = new URLSearchParams(paramsCopy);

    const pureURL = new URL(`${url}?${searchParams.toString()}`, clientHost);

    return pureURL.toString();
}

/**
 * Check if the page can be edited
 *
 * @export
 * @param {DotPage} page
 * @param {CurrentUser} currentUser
 * @param {DotExperiment} [experiment]
 * @return {*}  {boolean}
 */
export function computeCanEditPage(
    page: DotPage,
    currentUser: CurrentUser,
    experiment?: DotExperiment
): boolean {
    const pageCanBeEdited = page.canEdit;

    const isLocked = computePageIsLocked(page, currentUser);

    const editingBlockedByExperiment = [
        DotExperimentStatus.RUNNING,
        DotExperimentStatus.SCHEDULED
    ].includes(experiment?.status);

    return !!pageCanBeEdited && !isLocked && !editingBlockedByExperiment;
}

/**
 * Check if the page is locked
 *
 * @export
 * @param {DotPage} page
 * @param {CurrentUser} currentUser
 * @return {*}
 */
export function computePageIsLocked(page: DotPage, currentUser: CurrentUser): boolean {
    return !!page?.locked && page?.lockedBy !== currentUser?.userId;
}

/**
 * Map the containerStructure to a DotContainerMap
 *
 * @private
 * @param {DotPageContainerStructure} containers
 * @return {*}  {DotContainerMap}
 */
export function mapContainerStructureToDotContainerMap(
    containers: DotPageContainerStructure
): DotContainerMap {
    return Object.keys(containers).reduce((acc, id) => {
        acc[id] = containers[id].container;

        return acc;
    }, {});
}

/**
 * Map the containerStructure to an array
 *
 * @private
 * @param {DotPageContainerStructure} containers
 */
export const mapContainerStructureToArrayOfContainers = (containers: DotPageContainerStructure) => {
    return Object.keys(containers).reduce(
        (
            acc: {
                identifier: string;
                uuid: string;
                contentletsId: string[];
            }[],
            container
        ) => {
            const contentlets = containers[container].contentlets; // Get all contentlets from the container

            const contentletsKeys = Object.keys(contentlets); // This is the keys of uuids of the container

            contentletsKeys.forEach((key) => {
                acc.push({
                    identifier:
                        containers[container].container.path ??
                        containers[container].container.identifier,
                    uuid: key.replace('uuid-', ''),
                    contentletsId: contentlets[key].map((contentlet) => contentlet.identifier)
                });
            });

            return acc;
        },
        []
    );
};

/**
 * Get the host name for the request
 *
 * @export
 * @param {boolean} isTraditionalPage
 * @param {DotPageApiParams} params
 * @return {*}  {string}
 */
export const getRequestHostName = (isTraditionalPage: boolean, params: DotPageApiParams) => {
    return !isTraditionalPage ? params.clientHost : window.location.origin;
};

/**
 *  Get the error payload
 * @param errorCode
 * @returns {{code: number; pageInfo: CommonErrorsInfo | null}}
 */
export const getErrorPayload = (errorCode: number) =>
    errorCode
        ? {
              code: errorCode,
              pageInfo: COMMON_ERRORS[errorCode?.toString()] ?? null
          }
        : null;

/**
 * Get the editor states
 * @param state
 * @returns {{isDragging: boolean; dragIsActive: boolean; isScrolling: boolean}}
 */
export const getEditorStates = (state: EDITOR_STATE) => ({
    isDragging: state === EDITOR_STATE.DRAGGING,
    dragIsActive: state === EDITOR_STATE.DRAGGING || state === EDITOR_STATE.SCROLL_DRAG,
    isScrolling: state === EDITOR_STATE.SCROLL_DRAG || state === EDITOR_STATE.SCROLLING
});

/**
 * Compare two URL paths
 *
 * @param {string} urlPath
 * @param {string} urlPath2
 * @return {*}  {boolean}
 */
export const compareUrlPaths = (urlPath: string, urlPath2: string): boolean => {
    // Host doesn't matter here, we just need the pathname
    const { pathname: pathname1 } = new URL(urlPath, window.origin);
    const { pathname: pathname2 } = new URL(urlPath2, window.origin);

    return pathname1 === pathname2;
};

/**
 * Get the data from the drag dataset
 *
 * @param {DragDataset} dataset
 * @return {*}
 */
export const getDragItemData = ({ type, item }: DOMStringMap) => {
    try {
        const data = JSON.parse(item) as DragDatasetItem;
        const { contentType, contentlet, container, move } = data;

        if (type === 'content-type') {
            return {
                baseType: contentType.baseType,
                contentType: contentType.variable,
                draggedPayload: {
                    item: {
                        variable: contentType.variable,
                        name: contentType.name
                    },
                    type,
                    move
                } as ContentTypeDragPayload
            };
        }

        return {
            baseType: contentlet.baseType,
            contentType: contentlet.contentType,
            draggedPayload: {
                item: {
                    contentlet,
                    container
                },
                type,
                move
            } as ContentletDragPayload
        };
    } catch (error) {
        // It can fail if the data.item is not a valid JSON
        // In that case, we are draging an invalid element from the window
        return null;
    }
};
