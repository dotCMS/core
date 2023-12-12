import { Container, ContainerActionPayload } from '../shared/models';

/**
 * Insert a contentlet in a container
 *
 * @export
 * @param {ContainerActionPayload} {
 *     pageContainers,
 *     container,
 *     contentletID
 * }
 * @return {*}  {Container[]}
 */
export function insertContentletInContainer({
    pageContainers,
    container,
    contentletID,
    personaTag
}: ContainerActionPayload): Container[] {
    return pageContainers.map((currentContainer) => {
        if (
            areContainersEquals(currentContainer, container) &&
            !currentContainer.contentletsId.includes(contentletID)
        ) {
            currentContainer.contentletsId.push(contentletID);
        }

        currentContainer.personaTag = personaTag;

        return currentContainer;
    });
}

/**
 * Delete a contentlet from a container
 *
 * @export
 * @param {ContainerActionPayload} {
 *     pageContainers,
 *     container,
 *     contentletID
 * }
 * @return {*}  {Container[]}
 */
export function deleteContentletFromContainer({
    pageContainers,
    container,
    contentletID,
    personaTag
}: ContainerActionPayload): Container[] {
    return pageContainers.map((currentContainer) => {
        if (areContainersEquals(currentContainer, container)) {
            return {
                ...currentContainer,
                contentletsId: currentContainer.contentletsId.filter((id) => id !== contentletID),
                personaTag
            };
        }

        return currentContainer;
    });
}

/**
 * Check if two containers are equals
 *
 * @param {Container} currentContainer
 * @param {Container} containerToFind
 * @return {*}  {boolean}
 */
function areContainersEquals(currentContainer: Container, containerToFind: Container): boolean {
    return (
        currentContainer.identifier === containerToFind.identifier &&
        currentContainer.uuid === containerToFind.uuid
    );
}
