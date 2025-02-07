import { DotCMSColumnContainer, DotCMSPageAsset } from '../../types';

/**
 * Interface representing the data needed for container editing
 * @interface ContainerEditData
 */
interface ContainerEditData {
    uuid: string;
    identifier: string;
    acceptTypes: string;
    maxContentlets: number;
    variantId?: string;
}

/**
 * Retrieves container data from a DotCMS page asset using the container reference.
 * This function processes the container information and returns a standardized format
 * for container editing.
 *
 * @param {DotCMSPageAsset} dotCMSPageAsset - The page asset containing all containers data
 * @param {DotCMSColumnContainer} columContainer - The container reference from the layout
 * @throws {Error} When page asset is invalid or container is not found
 * @returns {ContainerEditData} Formatted container data for editing
 *
 * @example
 * const containerData = getContainersData(pageAsset, containerRef);
 * // Returns: { uuid: '123', identifier: 'cont1', acceptTypes: 'type1,type2', maxContentlets: 5 }
 */
export const getContainersData = (
    dotCMSPageAsset: DotCMSPageAsset,
    columContainer: DotCMSColumnContainer
): ContainerEditData | null => {
    const { identifier, uuid } = columContainer;
    const dotContainer = dotCMSPageAsset.containers[identifier];

    if (!dotContainer) {
        console.warn(`Container with identifier ${identifier} not found`);
        return null;
    }

    const { containerStructures, container } = dotContainer;

    const acceptTypes =
        containerStructures?.map((structure) => structure.contentTypeVar).join(',') ?? '';

    const variantId = container?.parentPermissionable?.variantId;
    const maxContentlets = container?.maxContentlets ?? 0;
    const path = container?.path;

    return {
        uuid,
        variantId,
        acceptTypes,
        maxContentlets,
        identifier: path ?? identifier
    };
};

/**
 * Retrieves the contentlets (content items) associated with a specific container.
 * Handles different UUID formats and provides warning for missing contentlets.
 *
 * @param {DotCMSPageAsset} dotCMSPageAsset - The page asset containing all containers data
 * @param {DotCMSColumnContainer} columContainer - The container reference from the layout
 * @returns {DotCMSContentlet[]} Array of contentlets in the container
 *
 * @example
 * const contentlets = getContentletsInContainer(pageAsset, containerRef);
 * // Returns: [{ identifier: 'cont1', ... }, { identifier: 'cont2', ... }]
 */
export const getContentletsInContainer = (
    dotCMSPageAsset: DotCMSPageAsset,
    columContainer: DotCMSColumnContainer
) => {
    const { identifier, uuid } = columContainer;
    const { contentlets } = dotCMSPageAsset.containers[identifier];
    const contentletsInContainer =
        contentlets[`uuid-${uuid}`] || contentlets[`uuid-dotParser_${uuid}`] || [];

    if (!contentletsInContainer) {
        console.warn(
            `We couldn't find the contentlets for the container with the identifier ${identifier} and the uuid ${uuid} becareful by adding content to this container.\nWe recommend to change the container in the layout and add the content again.`
        );
    }

    return contentletsInContainer;
};

/**
 * Generates the required DotCMS data attributes for a container element.
 * These attributes are used by DotCMS for container identification and functionality.
 *
 * @param {ContainerEditData} params - Container data including uuid, identifier, acceptTypes, and maxContentlets
 * @returns {Record<string, string | number>} Object containing all necessary data attributes
 *
 * @example
 * const attributes = getDotContainerAttributes({
 *   uuid: '123',
 *   identifier: 'cont1',
 *   acceptTypes: 'type1,type2',
 *   maxContentlets: 5
 * });
 * // Returns: { 'data-dot-object': 'container', 'data-dot-identifier': 'cont1', ... }
 */
export function getDotContainerAttributes({
    uuid,
    identifier,
    acceptTypes,
    maxContentlets
}: Record<string, any>): Record<string, any> {
    return {
        'data-testid': 'dot-container',
        'data-dot-object': 'container',
        'data-dot-accept-types': acceptTypes,
        'data-dot-identifier': identifier,
        'data-max-contentlets': maxContentlets,
        'data-dot-uuid': uuid
    };
}
