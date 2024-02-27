export function getPageElementBound(rowsNodes: HTMLDivElement[] | null) {
    if (!rowsNodes) {
        return [];
    }

    return rowsNodes.map((row) => {
        const rowRect = row.getBoundingClientRect();
        const columns = row.children;

        return {
            x: rowRect.x,
            y: rowRect.y,
            width: rowRect.width,
            height: rowRect.height,
            columns: Array.from(columns).map((column) => {
                const columnRect = column.getBoundingClientRect();
                const containers = Array.from(
                    column.querySelectorAll('[data-dot="container"]')
                ) as HTMLDivElement[];

                const columnX = columnRect.left - rowRect.left;
                const columnY = columnRect.top - rowRect.top;

                return {
                    x: columnX,
                    y: columnY,
                    width: columnRect.width,
                    height: columnRect.height,
                    containers: containers.map((container) => {
                        const containerRect = container.getBoundingClientRect();
                        const contentlets = Array.from(
                            container.querySelectorAll('[data-dot-object="contentlet"]')
                        ) as HTMLDivElement[];

                        return {
                            x: 0,
                            y: containerRect.y - rowRect.top,
                            width: containerRect.width,
                            height: containerRect.height,
                            payload: container.dataset?.['content'], //TODO: Change this later
                            contentlets: contentlets.map((contentlet) => {
                                const contentletRect = contentlet.getBoundingClientRect();

                                return {
                                    x: 0,
                                    y: contentletRect.y - containerRect.y,
                                    width: contentletRect.width,
                                    height: contentletRect.height,
                                    payload: JSON.stringify({
                                        container: contentlet.dataset?.['dotContainer']
                                            ? JSON.parse(contentlet.dataset?.['dotContainer'])
                                            : getContainerData(contentlet),
                                        contentlet: {
                                            identifier: contentlet.dataset?.['dotIdentifier'],
                                            title: contentlet.dataset?.['dotTitle'],
                                            inode: contentlet.dataset?.['dotInode'],
                                            contentType: contentlet.dataset?.['dotType']
                                        }
                                    })
                                };
                            })
                        };
                    })
                };
            })
        };
    });
}

export function getContainerData(element: Element) {
    // Find the closest ancestor element with data-dot-object="container" attribute
    const container = element.closest('[data-dot-object="container"]') as HTMLElement;

    // If a container element is found
    if (container) {
        // Return the dataset of the container element
        return {
            acceptTypes: container.dataset?.['dotAcceptTypes'],
            identifier: container.dataset?.['dotIdentifier'],
            maxContentlets: container.dataset?.['dotMaxContentlets'],
            uuid: container.dataset?.['dotUuid']
        };
    } else {
        // If no container element is found, return null
        console.warn('No container found for the contentlet');
        return null;
    }
}
