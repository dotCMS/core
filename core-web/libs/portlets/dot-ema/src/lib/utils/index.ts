import { ActionPayload, PageContainer } from '../shared/models';

/**
 * Insert a contentlet in a container
 *
 * @export
 * @param {ActionPayload} action
 * @return {*}  {PageContainer[]}
 */
export function insertContentletInContainer(action: ActionPayload): PageContainer[] {
    if (action.position && action.newContentletId) {
        return insertPositionedContentletInContainer(action);
    }

    const { pageContainers, container, contentlet, personaTag } = action;

    return pageContainers.map((currentContainer) => {
        if (
            areContainersEquals(currentContainer, container) &&
            !currentContainer.contentletsId.includes(contentlet.identifier)
        ) {
            currentContainer.contentletsId.push(contentlet.identifier);
        }

        currentContainer.personaTag = personaTag;

        return currentContainer;
    });
}

/**
 * Delete a contentlet from a container
 *
 * @export
 * @param {ActionPayload} action
 * @return {*}  {PageContainer[]}
 */
export function deleteContentletFromContainer(action: ActionPayload): PageContainer[] {
    const { pageContainers, container, contentlet, personaTag } = action;

    return pageContainers.map((currentContainer) => {
        if (areContainersEquals(currentContainer, container)) {
            return {
                ...currentContainer,
                contentletsId: currentContainer.contentletsId.filter(
                    (id) => id !== contentlet.identifier
                ),
                personaTag
            };
        }

        return currentContainer;
    });
}

/**
 * Check if two containers are equals
 *
 * @param {PageContainer} currentContainer
 * @param {PageContainer} containerToFind
 * @return {*}  {boolean}
 */
function areContainersEquals(
    currentContainer: PageContainer,
    containerToFind: PageContainer
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
 * @return {*}  {PageContainer[]}
 */
function insertPositionedContentletInContainer(payload: ActionPayload): PageContainer[] {
    const { pageContainers, container, contentlet, personaTag, newContentletId, position } =
        payload;

    return pageContainers.map((currentContainer) => {
        if (areContainersEquals(currentContainer, container)) {
            const index = currentContainer.contentletsId.indexOf(contentlet.identifier);

            if (index !== -1) {
                if (position === 'before') {
                    currentContainer.contentletsId.splice(index, 0, newContentletId);
                } else if (position === 'after') {
                    currentContainer.contentletsId.splice(index + 1, 0, newContentletId);
                }
            } else {
                currentContainer.contentletsId.push(newContentletId);
            }
        }

        currentContainer.personaTag = personaTag;

        return currentContainer;
    });
}
