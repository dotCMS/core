import {
    DotPageAssetLayoutColumn,
    DotCMSContentlet,
    DotCMSColumnContainer,
    DotCMSPageAsset
} from '../types';

/**
 * @internal
 *
 * Interface representing the data needed for container editing
 * @interface EditableContainerData
 */
interface EditableContainerData {
    uuid: string;
    identifier: string;
    acceptTypes: string;
    maxContentlets: number;
    variantId?: string;
}

/**
 * @internal
 *
 * Interface representing the data attributes of a DotCMS container.
 * @interface DotContainerAttributes
 */
export interface DotContainerAttributes {
    'data-dot-object': string;
    'data-dot-accept-types': string;
    'data-dot-identifier': string;
    'data-max-contentlets': string;
    'data-dot-uuid': string;
}

/**
 * @internal
 *
 * Interface representing the data attributes of a DotCMS contentlet.
 * @interface DotContentletAttributes
 */
export interface DotContentletAttributes {
    'data-dot-identifier': string;
    'data-dot-basetype': string;
    'data-dot-title': string;
    'data-dot-inode': string;
    'data-dot-type': string;
    'data-dot-container': string;
    'data-dot-on-number-of-pages': string;
}

const endClassMap: Record<number, string> = {
    1: 'col-end-1',
    2: 'col-end-2',
    3: 'col-end-3',
    4: 'col-end-4',
    5: 'col-end-5',
    6: 'col-end-6',
    7: 'col-end-7',
    8: 'col-end-8',
    9: 'col-end-9',
    10: 'col-end-10',
    11: 'col-end-11',
    12: 'col-end-12',
    13: 'col-end-13'
};

const startClassMap: Record<number, string> = {
    1: 'col-start-1',
    2: 'col-start-2',
    3: 'col-start-3',
    4: 'col-start-4',
    5: 'col-start-5',
    6: 'col-start-6',
    7: 'col-start-7',
    8: 'col-start-8',
    9: 'col-start-9',
    10: 'col-start-10',
    11: 'col-start-11',
    12: 'col-start-12'
};

/**
 * @internal
 *
 * Combine classes into a single string.
 *
 * @param {string[]} classes
 * @returns {string} Combined classes
 */
export const combineClasses = (classes: string[]) => classes.filter(Boolean).join(' ');

/**
 * @internal
 *
 * Calculates and returns the CSS Grid positioning classes for a column based on its configuration.
 * Uses a 12-column grid system where columns are positioned using grid-column-start and grid-column-end.
 *
 * @example
 * ```typescript
 * const classes = getColumnPositionClasses({
 *   leftOffset: 1, // Starts at the first column
 *   width: 6      // Spans 6 columns
 * });
 * // Returns: { startClass: 'col-start-1', endClass: 'col-end-7' }
 * ```
 *
 * @param {DotPageAssetLayoutColumn} column - Column configuration object
 * @param {number} column.leftOffset - Starting position (0-based) in the grid
 * @param {number} column.width - Number of columns to span
 * @returns {{ startClass: string, endClass: string }} Object containing CSS class names for grid positioning
 */
export const getColumnPositionClasses = (column: DotPageAssetLayoutColumn) => {
    const { leftOffset, width } = column;
    const startClass = startClassMap[leftOffset];
    const endClass = endClassMap[leftOffset + width];

    return {
        startClass,
        endClass
    };
};

/**
 * @internal
 *
 * Helper function that returns an object containing the dotCMS data attributes.
 * @param {DotCMSContentlet} contentlet - The contentlet to get the attributes for
 * @param {string} container - The container to get the attributes for
 * @returns {DotContentletAttributes} The dotCMS data attributes
 */
export function getDotContentletAttributes(
    contentlet: DotCMSContentlet,
    container: string
): DotContentletAttributes {
    return {
        'data-dot-identifier': contentlet?.identifier,
        'data-dot-basetype': contentlet?.baseType,
        'data-dot-title': contentlet?.widgetTitle || contentlet?.title,
        'data-dot-inode': contentlet?.inode,
        'data-dot-type': contentlet?.contentType,
        'data-dot-container': container,
        'data-dot-on-number-of-pages': contentlet?.onNumberOfPages
    };
}

/**
 * @internal
 *
 * Retrieves container data from a DotCMS page asset using the container reference.
 * This function processes the container information and returns a standardized format
 * for container editing.
 *
 * @param {DotCMSPageAsset} dotCMSPageAsset - The page asset containing all containers data
 * @param {DotCMSColumnContainer} columContainer - The container reference from the layout
 * @throws {Error} When page asset is invalid or container is not found
 * @returns {EditableContainerData} Formatted container data for editing
 *
 * @example
 * const containerData = getContainersData(pageAsset, containerRef);
 * // Returns: { uuid: '123', identifier: 'cont1', acceptTypes: 'type1,type2', maxContentlets: 5 }
 */
export const getContainersData = (
    dotCMSPageAsset: DotCMSPageAsset,
    columContainer: DotCMSColumnContainer
): EditableContainerData | null => {
    const { identifier, uuid } = columContainer;
    const dotContainer = dotCMSPageAsset.containers[identifier];

    if (!dotContainer) {
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
 * @internal
 *
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
 * @internal
 *
 * Generates the required DotCMS data attributes for a container element.
 * These attributes are used by DotCMS for container identification and functionality.
 *
 * @param {EditableContainerData} params - Container data including uuid, identifier, acceptTypes, and maxContentlets
 * @returns {DotContainerAttributes} Object containing all necessary data attributes
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
}: EditableContainerData): DotContainerAttributes {
    return {
        'data-dot-object': 'container',
        'data-dot-accept-types': acceptTypes,
        'data-dot-identifier': identifier,
        'data-max-contentlets': maxContentlets.toString(),
        'data-dot-uuid': uuid
    };
}
