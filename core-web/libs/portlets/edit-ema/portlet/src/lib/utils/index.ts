import { ActionPayload, ContainerPayload, PageContainer } from '../shared/models';

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
    containerToFind: ContainerPayload
): boolean {
    return (
        currentContainer.identifier === containerToFind.identifier &&
        currentContainer.uuid === containerToFind.uuid
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
export function sanitizeURL(url: string): string {
    return url
        .replace(/^\/|\/$/g, '') // Remove slashes from the beginning and end of the url
        .split('/')
        .filter((part, i) => {
            return !i || part !== 'index'; // Filter the index from the url if it is at the last position
        })
        .join('/');
}
