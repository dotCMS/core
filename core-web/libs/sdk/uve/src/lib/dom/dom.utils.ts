import { DotCMSContainerBound, DotCMSContentletBound } from '../types/editor/internal';

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

    if (
        element?.dataset?.['dotObject'] === 'contentlet' ||
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
