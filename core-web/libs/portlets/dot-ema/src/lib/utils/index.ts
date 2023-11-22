import { Container } from '../shared/models';

/**
 * Insert a contentlet in a container
 *
 * @param {{
 *         pageContainers: Container[];
 *         container: Container;
 *         contentletID: string;
 *     }} {
 *         pageContainers,
 *         container,
 *         contentletID
 *     }
 * @return {*}

 */
export function insertContentletInContainer({
    pageContainers,
    container,
    contentletID
}: {
    pageContainers: Container[];
    container: Container;
    contentletID: string;
}): Container[] {
    return pageContainers.map((currentContainer) => {
        areContainersEquals(currentContainer, container) &&
            !currentContainer.contentletsId.find((id) => id === contentletID) &&
            currentContainer.contentletsId.push(contentletID);

        return currentContainer;
    });
}

/**
 * Delete a contentlet from a container
 *
 * @param {{
 *         pageContainers: Container[];
 *         container: Container;
 *         contentletID: string;
 *     }} {
 *         pageContainers,
 *         container,
 *         contentletID
 *     }
 * @return {*}

 */
export function deleteContentletFromContainer({
    pageContainers,
    container,
    contentletID
}: {
    pageContainers: Container[];
    container: Container;
    contentletID: string;
}): Container[] {
    return pageContainers.map((currentContainer) => ({
        ...currentContainer,
        contentletsId: areContainersEquals(currentContainer, container)
            ? currentContainer.contentletsId.filter((id) => id !== contentletID)
            : currentContainer.contentletsId
    }));
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
