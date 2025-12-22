import { CurrentUser } from '@dotcms/dotcms-js';
import {
    DEFAULT_VARIANT_ID,
    DotCMSContentlet,
    DotContainerMap,
    DotDevice,
    DotExperiment,
    DotExperimentStatus
} from '@dotcms/dotcms-models';
import {
    DotCMSPage,
    DotCMSPageAssetContainers,
    DotCMSURLContentMap,
    DotCMSVanityUrl,
    DotCMSViewAsPersona
} from '@dotcms/types';

import { EmaDragItem } from '../edit-ema-editor/components/ema-page-dropzone/types';
import { DotPageApiParams } from '../services/dot-page-api.service';
import {
    BASE_IFRAME_MEASURE_UNIT,
    COMMON_ERRORS,
    DEFAULT_PERSONA,
    PERSONA_KEY
} from '../shared/consts';
import { EDITOR_STATE } from '../shared/enums';
import {
    ActionPayload,
    ContainerPayload,
    ContentletDragPayload,
    ContentTypeDragPayload,
    DotPageAssetParams,
    DragDatasetItem,
    PageContainer
} from '../shared/models';
import { Orientation } from '../store/models';

export const SDK_EDITOR_SCRIPT_SOURCE = '/ext/uve/dot-uve.js';

const REORDER_MENU_BASE_URL =
    'c/portal/layout?p_l_id=2df9f117-b140-44bf-93d7-5b10a36fb7f9&p_p_id=site-browser&p_p_action=1&p_p_state=maximized&_site_browser_struts_action=%2Fext%2Ffolders%2Forder_menu';

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
 *   errorCode?: 'CONTAINER_LIMIT_REACHED' | 'DUPLICATE_CONTENT';
 * }}
 */
export function insertContentletInContainer(action: ActionPayload): {
    pageContainers: PageContainer[];
    didInsert: boolean;
    errorCode?: 'CONTAINER_LIMIT_REACHED' | 'DUPLICATE_CONTENT';
} {
    if (action.position) {
        return insertPositionedContentletInContainer(action);
    }

    let didInsert = false;
    let errorCode: 'CONTAINER_LIMIT_REACHED' | 'DUPLICATE_CONTENT' | undefined;

    const { pageContainers, container, personaTag, newContentletId } = action;

    const containerIsOnPageResponse = pageContainers.find((pageContainer) =>
        areContainersEquals(pageContainer, container)
    );

    // We had a case where users are using the #parseContainer macro to hard code containers on their themes
    // This case was not taken into account when we moved to design templates, so the container is not getting indexed on the PageAPI until we add some content
    // That means, we have to trust the data we got from our SDK when we are adding a contentlet to a container and add the container to the pageContainers array if it's not there
    // https://github.com/dotCMS/core/issues/31790#issuecomment-2945998795
    if (!containerIsOnPageResponse) {
        pageContainers.push({
            ...container,
            contentletsId: [...(container.contentletsId ?? [])]
        });
    }

    const newPageContainers = pageContainers.map((pageContainer) => {
        if (areContainersEquals(pageContainer, container)) {
            // Check if content already exists (duplicate)
            if (pageContainer.contentletsId.includes(newContentletId)) {
                errorCode = 'DUPLICATE_CONTENT';
                return pageContainer;
            }

            // Validate container limit before adding
            const maxContentlets = container.maxContentlets;
            if (maxContentlets && pageContainer.contentletsId.length >= maxContentlets) {
                // Container is at or over its limit, don't add
                errorCode = 'CONTAINER_LIMIT_REACHED';
                return pageContainer;
            }

            pageContainer.contentletsId.push(newContentletId);
            didInsert = true;
        }

        pageContainer.personaTag = personaTag;

        return pageContainer;
    });

    return {
        pageContainers: newPageContainers,
        didInsert,
        errorCode
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

    const containerIsOnPageResponse = pageContainers.find((pageContainer) =>
        areContainersEquals(pageContainer, container)
    );

    // We had a case where users are using the #parseContainer macro to hard code containers on their themes
    // This case was not taken into account when we moved to design templates, so the container is not getting indexed on the PageAPI until we add some content
    // That means, we have to trust the data we got from our SDK when we are adding a contentlet to a container and add the container to the pageContainers array if it's not there
    // https://github.com/dotCMS/core/issues/31790#issuecomment-2945998795
    if (!containerIsOnPageResponse) {
        pageContainers.push({
            ...container,
            contentletsId: [...(container.contentletsId ?? [])]
        });
    }

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

        return {
            ...currentContainer,
            personaTag
        };
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
 *   errorCode?: 'CONTAINER_LIMIT_REACHED' | 'DUPLICATE_CONTENT';
 * }}
 */
function insertPositionedContentletInContainer(payload: ActionPayload): {
    pageContainers: PageContainer[];
    didInsert: boolean;
    errorCode?: 'CONTAINER_LIMIT_REACHED' | 'DUPLICATE_CONTENT';
} {
    let didInsert = false;
    let errorCode: 'CONTAINER_LIMIT_REACHED' | 'DUPLICATE_CONTENT' | undefined;

    const { pageContainers, container, contentlet, personaTag, newContentletId, position } =
        payload;

    const containerIsOnPageResponse = pageContainers.find((pageContainer) =>
        areContainersEquals(pageContainer, container)
    );

    // We had a case where users are using the #parseContainer macro to hard code containers on their themes
    // This case was not taken into account when we moved to design templates, so the container is not getting indexed on the PageAPI until we add some content
    // That means, we have to trust the data we got from our SDK when we are adding a contentlet to a container and add the container to the pageContainers array if it's not there
    // https://github.com/dotCMS/core/issues/31790#issuecomment-2945998795
    if (!containerIsOnPageResponse) {
        pageContainers.push({
            ...container,
            contentletsId: [...(container.contentletsId ?? [])]
        });
    }

    const newPageContainers = pageContainers.map((pageContainer) => {
        if (areContainersEquals(pageContainer, container)) {
            // Check if content already exists (duplicate)
            if (pageContainer.contentletsId.includes(newContentletId)) {
                errorCode = 'DUPLICATE_CONTENT';
                return pageContainer;
            }

            // Validate container limit before adding
            const maxContentlets = container.maxContentlets;
            if (maxContentlets && pageContainer.contentletsId.length >= maxContentlets) {
                // Container is at or over its limit, don't add
                errorCode = 'CONTAINER_LIMIT_REACHED';
                return pageContainer;
            }

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
        didInsert,
        errorCode
    };
}

/**
 * Sanitizes a URL by removing query parameters and cleaning up multiple slashes
 * @param url The URL to sanitize
 * @returns The sanitized URL path
 */
export function sanitizeURL(url?: string): string {
    if (!url || url === '/') {
        return '/';
    }

    // Remove query params if present
    const path = url.split('?')[0];

    return path.replace(/\/+/g, '/'); // Convert multiple slashes to single slash
}

/**
 * Get the personalization for the contentlet
 *
 * @param {DotCMSViewAsPersona} persona
 * @return {*}
 */
export const getPersonalization = (persona: DotCMSViewAsPersona) => {
    if (!persona || (!persona.contentType && !persona.keyTag)) {
        return `dot:default`;
    }

    return `dot:${persona.contentType}:${persona.keyTag}`;
};

/**
 * Constructs a full page URL by appending query parameters.
 *
 * This function takes a base URL and a set of parameters, optionally normalizing them,
 * and returns a complete URL with the query parameters appended. It ensures that any
 * undefined values are removed and that the 'url' parameter is not included in the query string.
 *
 * @export
 * @param {FullPageURLParams} { url, params, userFriendlyParams = false } - The parameters for constructing the URL.
 * @param {string} url - The base URL to which query parameters will be appended.
 * @param {DotPageAssetParams} params - The query parameters to append to the URL.
 * @param {boolean} [userFriendlyParams=false] - If true, the query parameters are normalized to be more readable and user-friendly. This may involve renaming keys or removing default values that are not necessary for end-users.
 * @return {string} The full URL with query parameters appended.
 */
export function getFullPageURL({
    url,
    params,
    userFriendlyParams = false
}: {
    url: string;
    params: DotPageAssetParams;
    userFriendlyParams?: boolean;
}): string {
    const searchParams = userFriendlyParams ? normalizeQueryParams(params) : { ...params };

    // Remove 'url' from query parameters if present
    if (searchParams.url) {
        delete searchParams['url'];
    }

    if (searchParams.clientHost) {
        delete searchParams['clientHost'];
    }

    // Filter out undefined values from query parameters
    Object.keys(searchParams).forEach(
        (key) => searchParams[key] === undefined && delete searchParams[key]
    );

    const path = cleanPageURL(url);
    const paramsAsString = new URLSearchParams({
        ...searchParams
    }).toString();

    return paramsAsString ? `${path}?${paramsAsString}` : path;
}

/**
 * Cleans and transforms query parameters for better readability and usability.
 *
 * This function processes the given query parameters by removing unnecessary values
 * (e.g., default identifiers) and renaming specific keys for a more user-friendly format.
 * Additional transformations can be applied as needed.
 *
 * @export
 * @param {Object} params - The raw query parameters to be processed.
 * @param {string} baseClientHost - The base client host to be used to compare with the clientHost query param.
 * @return {Object} A cleaned and formatted version of the query parameters.
 */
export function normalizeQueryParams(params, baseClientHost?: string) {
    const queryParams = { ...params };

    if (queryParams[PERSONA_KEY] === DEFAULT_PERSONA.identifier) {
        delete queryParams[PERSONA_KEY];
    }

    if (queryParams[PERSONA_KEY]) {
        queryParams['personaId'] = params[PERSONA_KEY];
        delete queryParams[PERSONA_KEY];
    }

    if (
        baseClientHost &&
        new URL(baseClientHost).toString() === new URL(params.clientHost).toString()
    ) {
        delete queryParams.clientHost;
    }

    return queryParams;
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
 * @param {DotCMSVanityUrl} vanityUrl
 * @return {*}
 */
export function isForwardOrPage(vanityUrl?: DotCMSVanityUrl): boolean {
    if (!vanityUrl) {
        return true;
    }

    const pageAPIPropsExist = 'permanentRedirect' in vanityUrl && 'temporaryRedirect' in vanityUrl;

    if (pageAPIPropsExist) {
        return !vanityUrl?.permanentRedirect && !vanityUrl?.temporaryRedirect;
    }

    return vanityUrl?.action === 200 || vanityUrl?.response === 200; // GraphQL API returns 200 for forward
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

    const clientHost = paramsCopy?.clientHost || window.location.origin;
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
 * Checks if a page is locked by a different user (not the current user).
 *
 * @param {DotCMSPage} page - The page to check
 * @param {CurrentUser} currentUser - The current user
 * @return {boolean} True if page is locked by another user
 */
export function isPageLockedByOtherUser(page: DotCMSPage, currentUser: CurrentUser): boolean {
    return !!page?.locked && page?.lockedBy !== currentUser?.userId;
}

/**
 * Checks if the page is locked.
 *
 * With feature flag enabled: Returns true if page is locked by ANY user
 * With feature flag disabled: Returns true if page is locked by ANOTHER user
 *
 * @param {DotCMSPage} page - The page to check
 * @param {CurrentUser} currentUser - The current user
 * @param {boolean} isFeatureFlagEnabled - Whether the lock toggle feature is enabled
 * @return {boolean} True if page is considered locked based on feature flag
 */
export function computeIsPageLocked(
    page: DotCMSPage,
    currentUser: CurrentUser,
    isFeatureFlagEnabled: boolean
): boolean {
    if (isFeatureFlagEnabled) {
        return !!page?.locked;
    }

    // This is the legacy behavior, only show "locked" button if it is locked by another user
    const isLocked = isPageLockedByOtherUser(page, currentUser);
    return isLocked;
}

/**
 * Determines if the current user can edit the page.
 *
 * Editing is allowed when ALL of the following are true:
 * - User has edit permission on the page
 * - Page is not locked (or locked by current user with feature flag enabled)
 * - No experiment is running or scheduled
 *
 * @param {DotCMSPage} page - The page to check
 * @param {CurrentUser} currentUser - The current user
 * @param {DotExperiment} [experiment] - Optional experiment data
 * @param {boolean} [isFeatureFlagEnabled=false] - Whether the lock toggle feature is enabled
 * @return {boolean} True if user can edit the page
 */
export function computeCanEditPage(
    page: DotCMSPage,
    currentUser: CurrentUser,
    experiment?: DotExperiment,
    isFeatureFlagEnabled = false
): boolean {
    const hasEditPermission = !!page?.canEdit;

    const isBlockedByExperiment = [
        DotExperimentStatus.RUNNING,
        DotExperimentStatus.SCHEDULED
    ].includes(experiment?.status);

    if (!hasEditPermission || isBlockedByExperiment) {
        return false;
    }

    if (isFeatureFlagEnabled) {
        // Always can access to Draft mode (edit) if feature flag is enabled
        return true;
    }

    // Legacy behavior: user can access to Draft mode (edit) if page is not locked by another user
    const isLocked = computeIsPageLocked(page, currentUser, isFeatureFlagEnabled);
    // If the page is locked, the user cannot access to Draft mode (edit)
    return !isLocked;
}

/**
 * Map the containerStructure to a DotContainerMap
 *
 * @private
 * @param {DotCMSPageAssetContainers} containers
 * @return {*}  {DotContainerMap}
 */
export function mapContainerStructureToDotContainerMap(
    containers: DotCMSPageAssetContainers
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
 * @param {DotCMSPageAssetContainers} containers
 */
export const mapContainerStructureToArrayOfContainers = (containers: DotCMSPageAssetContainers) => {
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
export const getRequestHostName = (params: DotPageApiParams) => {
    return params?.clientHost || window.location.origin;
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
    } catch {
        // It can fail if the data.item is not a valid JSON
        // In that case, we are draging an invalid element from the window
        return null;
    }
};

/**
 * Adds missing query parameters `pagePath` and `hostId` to the given URL if they are not already present.
 *
 * @param {Object} params - The parameters object.
 * @param {string} params.url - The URL to which the parameters will be added.
 * @param {string} params.pagePath - The page path to be added as a query parameter if missing.
 * @param {string} params.hostId - The host ID to be added as a query parameter if missing.
 * @returns {string} - The updated URL with the missing parameters added.
 */
export const createReorderMenuURL = ({
    startLevel,
    depth,
    pagePath,
    hostId
}: {
    startLevel: number;
    depth: number;
    pagePath: string;
    hostId: string;
}) => {
    const urlObject = new URL(REORDER_MENU_BASE_URL, window.location.origin);

    const params = urlObject.searchParams;

    if (!params.has('startLevel')) {
        params.set('startLevel', startLevel.toString());
    }

    if (!params.has('depth')) {
        params.set('depth', depth.toString());
    }

    if (!params.has('pagePath')) {
        params.set('pagePath', pagePath);
    }

    if (!params.has('hostId')) {
        params.set('hostId', hostId);
    }

    return urlObject.toString();
};

/**
 * Check if the clientHost is in the whitelist provided by the app
 *
 * @private
 * @param {string} clientHost
 * @param {*} [allowedDevURLs=[]]
 * @return {*}
 * @memberof DotEmaShellComponent
 */
export const checkClientHostAccess = (
    clientHost: string,
    allowedDevURLs: string[] = []
): boolean => {
    if (!clientHost || !Array.isArray(allowedDevURLs) || !allowedDevURLs.length) {
        return false;
    }

    // Most IDEs and terminals add a / at the end of the URL, so we need to sanitize it
    const sanitizedClientHost = new URL(clientHost).toString();
    const sanitizedAllowedDevURLs = allowedDevURLs.map((url) => new URL(url).toString());

    return sanitizedAllowedDevURLs.includes(sanitizedClientHost);
};

/**
 * Determines the target URL for navigation.
 *
 * If `urlContentMap` is present and contains a `URL_MAP_FOR_CONTENT`, it will be used.
 * Otherwise, it falls back to the URL extracted from the event.
 *
 * @param {string | undefined} url - The URL extracted from the event.
 * @returns {string | undefined} - The final target URL for navigation, or undefined if none.
 */
export function getTargetUrl(
    url: string | undefined,
    urlContentMap: DotCMSURLContentMap
): string | undefined {
    // Return URL from content map or fallback to the provided URL
    return urlContentMap?.URL_MAP_FOR_CONTENT || url;
}

/**
 * Determines whether navigation to a new URL is necessary.
 *
 * @param {string | undefined} targetUrl - The target URL for navigation.
 * @returns {boolean} - True if the current URL differs from the target URL and navigation is required.
 */
export function shouldNavigate(targetUrl: string | undefined, currentUrl: string): boolean {
    // Navigate if the target URL is defined and different from the current URL
    return targetUrl !== undefined && !compareUrlPaths(targetUrl, currentUrl);
}

/**
 * Get the page URI from the contentlet
 *
 * If the URL_MAP_FOR_CONTENT is present, it will be used as the page URI.
 *
 * @param {DotCMSContentlet} { urlContentMap, pageURI, url}
 * @return {*}  {string}
 */
export const getPageURI = ({ urlContentMap, pageURI, url }: DotCMSContentlet): string => {
    const contentMapUrl = urlContentMap?.URL_MAP_FOR_CONTENT;
    const pageURIUrl = pageURI ?? url;
    const newUrl = contentMapUrl ?? pageURIUrl;

    return sanitizeURL(newUrl);
};

export const getOrientation = (device: DotDevice): Orientation => {
    return Number(device?.cssHeight) > Number(device?.cssWidth)
        ? Orientation.PORTRAIT
        : Orientation.LANDSCAPE;
};

export const getWrapperMeasures = (
    device: DotDevice,
    orientation?: Orientation
): { width: string; height: string } => {
    const unit = device?.inode !== 'default' ? BASE_IFRAME_MEASURE_UNIT : '%';

    return orientation === Orientation.LANDSCAPE
        ? {
              width: `${Math.max(Number(device?.cssHeight), Number(device?.cssWidth))}${unit}`,
              height: `${Math.min(Number(device?.cssHeight), Number(device?.cssWidth))}${unit}`
          }
        : {
              width: `${Math.min(Number(device?.cssHeight), Number(device?.cssWidth))}${unit}`,
              height: `${Math.max(Number(device?.cssHeight), Number(device?.cssWidth))}${unit}`
          };
};

/**
 * Cleans and normalizes a page URL by:
 * 1. Removing leading slashes while preserving trailing slash if present
 * 2. Converting multiple consecutive slashes at end into single slashes
 *
 * @param {string} url - The URL to clean
 * @returns {string} The cleaned URL with normalized slashes
 */
export const cleanPageURL = (url: string) => {
    return url
        .replace(/^\/*(.*?)(\/+)?$/, '$1$2') // Capture content and optional trailing slash
        .replace(/\/+/g, '/'); // Clean up any remaining multiple slashes
};

/**
 * Converts a Date object to an ISO 8601 string in UTC, preserving the local time
 * but expressing it in UTC timezone.
 * @param {Date} date - Reference Date object
 * @param {boolean} [includeMilliseconds=false] - If true, includes milliseconds
 * @returns {string} String in ISO 8601 format with the date in UTC
 */
export const convertLocalTimeToUTC = (date: Date, includeMilliseconds = false) => {
    // Validate parameters
    if (!(date instanceof Date)) {
        throw new Error('Parameter must be a Date object');
    }

    // Extract local time from the date
    const hours = date.getHours();
    const minutes = date.getMinutes();
    const seconds = date.getSeconds();
    const milliseconds = date.getMilliseconds();

    // Create new UTC date with the same local date and time
    const utcDate = new Date(
        Date.UTC(
            date.getFullYear(),
            date.getMonth(),
            date.getDate(),
            hours,
            minutes,
            seconds,
            includeMilliseconds ? milliseconds : 0
        )
    );

    // Return in ISO 8601 format
    const isoString = utcDate.toISOString();

    // Optionally remove milliseconds
    return includeMilliseconds ? isoString : isoString.replace(/\.\d{3}Z$/, 'Z');
};

/**
 * Converts a Date object (representing a UTC time) to a Local Date object
 * where the Local time matches the UTC time of the input.
 * This is the inverse of convertLocalTimeToUTC.
 * @param {Date} date - Reference Date object (treated as UTC)
 * @returns {Date} Date object where Local time matches input's UTC time
 */
export const convertUTCToLocalTime = (date: Date) => {
    if (!(date instanceof Date)) {
        throw new Error('Parameter must be a Date object');
    }

    return new Date(
        date.getUTCFullYear(),
        date.getUTCMonth(),
        date.getUTCDate(),
        date.getUTCHours(),
        date.getUTCMinutes(),
        date.getUTCSeconds(),
        date.getUTCMilliseconds()
    );
};

export const removeUndefinedValues = (params: DotPageAssetParams) => {
    return Object.fromEntries(Object.entries(params).filter(([_, value]) => value !== undefined));
};

/**
 * Convert the client params to the page params
 *
 * @param {*} params
 * @return {*}
 */
export const convertClientParamsToPageParams = (params) => {
    if (!params) {
        return null;
    }

    const { personaId, languageId, ...rest } = params;
    const pageParams = {
        ...rest,
        [PERSONA_KEY]: personaId,
        language_id: languageId
    };

    return removeUndefinedValues(pageParams);
};
