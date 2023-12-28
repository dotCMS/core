import { ActionPayload, ContainerPayload, PageContainer } from '../shared/models';

/**
 * Insert a contentlet in a container
 *
 * @export
 * @param {ActionPayload} action
 * @return {*}  {PageContainer[]}
 */
export function insertContentletInContainer(action: ActionPayload): PageContainer[] {
    if (action.position) {
        return insertPositionedContentletInContainer(action);
    }

    const { pageContainers, container, personaTag, newContentletId } = action;

    return pageContainers.map((pageContainer) => {
        if (
            areContainersEquals(pageContainer, container) &&
            !pageContainer.contentletsId.includes(newContentletId)
        ) {
            pageContainer.contentletsId.push(newContentletId);
        }

        pageContainer.personaTag = personaTag;

        return pageContainer;
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
 * @param {ContainerPayload} containerToFind
 * @return {*}  {boolean}
 */
function areContainersEquals(
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
 * @return {*}  {PageContainer[]}
 */
function insertPositionedContentletInContainer(payload: ActionPayload): PageContainer[] {
    const { pageContainers, container, contentlet, personaTag, newContentletId, position } =
        payload;

    return pageContainers.map((pageContainer) => {
        if (areContainersEquals(pageContainer, container)) {
            const index = pageContainer.contentletsId.indexOf(contentlet.identifier);

            if (index !== -1) {
                const offset = position === 'before' ? index : index + 1;
                pageContainer.contentletsId.splice(offset, 0, newContentletId);
            } else {
                pageContainer.contentletsId.push(newContentletId);
            }
        }

        pageContainer.personaTag = personaTag;

        return pageContainer;
    });
}
