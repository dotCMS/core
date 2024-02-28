export function getPageElementBound(containers: HTMLDivElement[]) {
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
            payload: container.dataset?.['content'] ?? {
                container: getContainerData(container)
            },
            contentlets: getContentletsBound(containerRect, contentlets)
        };
    });
}

export function getContentletsBound(containerRect: DOMRect, contentlets: HTMLDivElement[]) {
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
//Used to get container data from VTLS.
export function getContainerData(container: HTMLElement) {
    return {
        acceptTypes: container.dataset?.['dotAcceptTypes'],
        identifier: container.dataset?.['dotIdentifier'],
        maxContentlets: container.dataset?.['maxContentlets'],
        uuid: container.dataset?.['dotUuid']
    };
}

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
//TODO: Fix typeLater
// USed to find contentlets and later add the listeners "onHover"
export function findContentletElement(element: HTMLElement | null) {
    if (!element) return null;

    if (element.dataset && element.dataset?.['dotObject'] === 'contentlet') {
        return element;
    } else {
        return findContentletElement(element?.['parentElement']);
    }
}
