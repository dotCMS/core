/**
 * Bound information for a contentlet.
 *
 * @interface ContentletBound
 */
interface ContentletBound {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: string;
}

/**
 * Bound information for a container.
 *
 * @interface ContainerBound
 */
interface ContainerBound {
    x: number;
    y: number;
    width: number;
    height: number;
    payload: string;
    contentlets: ContentletBound[];
}

/**
 * Calculates the bounding information for each page element within the given containers.
 *
 * @export
 * @param {HTMLDivElement[]} containers
 * @return {*} An array of objects containing the bounding information for each page element.
 */
export function getPageElementBound(containers: HTMLDivElement[]): ContainerBound[] {
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
                container: getContainerData(container)
            }),
            contentlets: getContentletsBound(containerRect, contentlets)
        };
    });
}

/**
 * An array of objects containing the bounding information for each contentlet inside a container.
 *
 * @export
 * @param {DOMRect} containerRect
 * @param {HTMLDivElement[]} contentlets
 * @return {*}
 */
export function getContentletsBound(
    containerRect: DOMRect,
    contentlets: HTMLDivElement[]
): ContentletBound[] {
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
                    : getClosestContainerData(contentlet),
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
 * @param {HTMLElement} container
 * @return {*}
 */
export function getContainerData(container: HTMLElement) {
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
 * @param {Element} element
 * @return {*}
 */
export function getClosestContainerData(element: Element) {
    // Find the closest ancestor element with data-dot-object="container" attribute
    const container = element.closest('[data-dot-object="container"]') as HTMLElement;

    // If a container element is found
    if (container) {
        // Return the dataset of the container element
        return getContainerData(container);
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
 * @param {(HTMLElement | null)} element
 * @return {*}
 */
export function findDotElement(element: HTMLElement | null): HTMLElement | null {
    if (!element) return null;

    if (
        element?.dataset?.['dotObject'] === 'contentlet' ||
        (element?.dataset?.['dotObject'] === 'container' && element.children.length === 0)
    ) {
        return element;
    }

    return findDotElement(element?.['parentElement']);
}

export function findDotVTLElement(element: HTMLElement | null): HTMLElement | null {
    if (!element) return null;

    if (element.dataset && element.dataset?.['dotObject'] === 'vtl-file') {
        return element;
    } else {
        return findDotElement(element?.['parentElement']);
    }
}

export function findVTLData(target: HTMLElement) {
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
