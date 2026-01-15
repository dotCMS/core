import {
    DotCMSBasicContentlet,
    DotCMSColumnContainer,
    DotCMSPageAsset,
    DotPageAssetLayoutColumn,
    EditableContainerData
} from '@dotcms/types';
import {
    DotCMSContainerBound,
    DotCMSContentletBound,
    DotContainerAttributes,
    DotContentletAttributes
} from '@dotcms/types/internal';

import { END_CLASS, START_CLASS } from '../../internal/constants';

/**
 * Calculates the bounding information for each page element within the given containers.
 *
 * @export
 * @param {HTMLDivElement[]} containers - An array of HTMLDivElement representing the containers.
 * @return {DotCMSContainerBound[]} An array of objects containing the bounding information for each page element.
 * @example
 * ```ts
 * const containers = document.querySelectorAll('.container');
 * const bounds = getDotCMSPageBounds(containers);
 * console.log(bounds);
 * ```
 */
export function getDotCMSPageBounds(containers: HTMLDivElement[]): DotCMSContainerBound[] {
    return containers.map((container) => {
        const containerRect = container.getBoundingClientRect();
        const contentlets = Array.from(
            container.querySelectorAll('[data-dot-object="contentlet"]')
        ) as HTMLDivElement[];

        return {
            x: containerRect.x,
            y: containerRect.y,
            width: containerRect.width,
            height: containerRect.height,
            payload: JSON.stringify({
                container: getDotCMSContainerData(container)
            }),
            contentlets: getDotCMSContentletsBound(containerRect, contentlets)
        };
    });
}

/**
 * Calculates the bounding information for each contentlet inside a container.
 *
 * @export
 * @param {DOMRect} containerRect - The bounding rectangle of the container.
 * @param {HTMLDivElement[]} contentlets - An array of HTMLDivElement representing the contentlets.
 * @return {DotCMSContentletBound[]} An array of objects containing the bounding information for each contentlet.
 * @example
 * ```ts
 * const containerRect = container.getBoundingClientRect();
 * const contentlets = container.querySelectorAll('.contentlet');
 * const bounds = getDotCMSContentletsBound(containerRect, contentlets);
 * console.log(bounds); // Element bounds within the container
 * ```
 */
export function getDotCMSContentletsBound(
    containerRect: DOMRect,
    contentlets: HTMLDivElement[]
): DotCMSContentletBound[] {
    return contentlets.map((contentlet) => {
        const contentletRect = contentlet.getBoundingClientRect();

        return {
            x: 0,
            y: contentletRect.y - containerRect.y,
            width: contentletRect.width,
            height: contentletRect.height,
            payload: JSON.stringify({
                container: contentlet.dataset?.['dotContainer']
                    ? JSON.parse(contentlet.dataset?.['dotContainer'])
                    : getClosestDotCMSContainerData(contentlet),
                contentlet: {
                    identifier: contentlet.dataset?.['dotIdentifier'],
                    title: contentlet.dataset?.['dotTitle'],
                    inode: contentlet.dataset?.['dotInode'],
                    contentType: contentlet.dataset?.['dotType']
                }
            })
        };
    });
}

/**
 * Get container data from VTLS.
 *
 * @export
 * @param {HTMLElement} container - The container element.
 * @return {object} An object containing the container data.
 * @example
 * ```ts
 * const container = document.querySelector('.container');
 * const data = getContainerData(container);
 * console.log(data);
 * ```
 */
export function getDotCMSContainerData(container: HTMLElement) {
    return {
        acceptTypes: container.dataset?.['dotAcceptTypes'] || '',
        identifier: container.dataset?.['dotIdentifier'] || '',
        maxContentlets: container.dataset?.['maxContentlets'] || '',
        uuid: container.dataset?.['dotUuid'] || ''
    };
}

/**
 * Get the closest container data from the contentlet.
 *
 * @export
 * @param {Element} element - The contentlet element.
 * @return {object | null} An object containing the closest container data or null if no container is found.
 * @example
 * ```ts
 * const contentlet = document.querySelector('.contentlet');
 * const data = getClosestDotCMSContainerData(contentlet);
 * console.log(data);
 * ```
 */
export function getClosestDotCMSContainerData(element: Element) {
    // Find the closest ancestor element with data-dot-object="container" attribute
    const container = element.closest('[data-dot-object="container"]') as HTMLElement;

    // If a container element is found
    if (container) {
        // Return the dataset of the container element
        return getDotCMSContainerData(container);
    } else {
        // If no container element is found, return null
        console.warn('No container found for the contentlet');

        return null;
    }
}

/**
 * Find the closest contentlet element based on HTMLElement.
 *
 * @export
 * @param {HTMLElement | null} element - The starting element.
 * @return {HTMLElement | null} The closest contentlet element or null if not found.
 * @example
 * const element = document.querySelector('.some-element');
 * const contentlet = findDotCMSElement(element);
 * console.log(contentlet);
 */
export function findDotCMSElement(element: HTMLElement | null): HTMLElement | null {
    if (!element) return null;

    const emptyContent = element.querySelector('[data-dot-object="empty-content"]');

    if (
        element?.dataset?.['dotObject'] === 'contentlet' ||
        // The container inside Headless components have a span with the data-dot-object="container" attribute
        (element?.dataset?.['dotObject'] === 'container' && emptyContent) ||
        // The container inside Traditional have no content inside
        (element?.dataset?.['dotObject'] === 'container' && element.children.length === 0)
    ) {
        return element;
    }

    return findDotCMSElement(element?.['parentElement']);
}

/**
 * Find VTL data within a target element.
 *
 * @export
 * @param {HTMLElement} target - The target element to search within.
 * @return {Array<{ inode: string, name: string }> | null} An array of objects containing VTL data or null if none found.
 * @example
 * ```ts
 * const target = document.querySelector('.target-element');
 * const vtlData = findDotCMSVTLData(target);
 * console.log(vtlData);
 * ```
 */
export function findDotCMSVTLData(target: HTMLElement) {
    const vltElements = target.querySelectorAll(
        '[data-dot-object="vtl-file"]'
    ) as NodeListOf<HTMLElement>;

    if (!vltElements.length) {
        return null;
    }

    return Array.from(vltElements).map((vltElement) => {
        return {
            inode: vltElement.dataset?.['dotInode'],
            name: vltElement.dataset?.['dotUrl']
        };
    });
}

/**
 * Check if the scroll position is at the bottom of the page.
 *
 * @export
 * @return {boolean} True if the scroll position is at the bottom, otherwise false.
 * @example
 * ```ts
 * if (dotCMSScrollIsInBottom()) {
 *     console.log('Scrolled to the bottom');
 * }
 * ```
 */
export function computeScrollIsInBottom() {
    const documentHeight = document.documentElement.scrollHeight;
    const viewportHeight = window.innerHeight;
    const scrollY = window.scrollY;

    return scrollY + viewportHeight >= documentHeight;
}

/**
 *
 *
 * Combine classes into a single string.
 *
 * @param {string[]} classes
 * @returns {string} Combined classes
 */
export const combineClasses = (classes: string[]): string => classes.filter(Boolean).join(' ');

/**
 *
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
    const startClass = `${START_CLASS}${leftOffset}`;
    const endClass = `${END_CLASS}${leftOffset + width}`;

    return {
        startClass,
        endClass
    };
};

/**
 *
 *
 * Helper function that returns an object containing the dotCMS data attributes.
 * @param {DotCMSBasicContentlet} contentlet - The contentlet to get the attributes for
 * @param {string} container - The container to get the attributes for
 * @returns {DotContentletAttributes} The dotCMS data attributes
 */
export function getDotContentletAttributes(
    contentlet: DotCMSBasicContentlet,
    container: string
): DotContentletAttributes {
    return {
        'data-dot-identifier': contentlet?.identifier,
        'data-dot-basetype': contentlet?.baseType,
        'data-dot-title': contentlet?.['widgetTitle'] || contentlet?.title,
        'data-dot-inode': contentlet?.inode,
        'data-dot-type': contentlet?.contentType,
        'data-dot-container': container,
        'data-dot-on-number-of-pages': contentlet?.['onNumberOfPages'] || '1',
        ...(contentlet?.styleProperties && {
            'data-dot-style-properties': JSON.stringify(contentlet.styleProperties)
        })
    };
}

/**
 *
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
 *
 *
 * Retrieves the contentlets (content items) associated with a specific container.
 * Handles different UUID formats and provides warning for missing contentlets.
 *
 * @param {DotCMSPageAsset} dotCMSPageAsset - The page asset containing all containers data
 * @param {DotCMSColumnContainer} columContainer - The container reference from the layout
 * @returns {DotCMSBasicContentlet[]} Array of contentlets in the container
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
 *
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
