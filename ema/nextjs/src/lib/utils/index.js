export function getPageElementBound(rowsNodes) {
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
                const containers = Array.from(column.querySelectorAll('[data-dot="container"]'));

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
                            container.querySelectorAll('[data-dot="contentlet"]')
                        );

                        return {
                            x: 0,
                            y: containerRect.y - rowRect.top,
                            width: containerRect.width,
                            height: containerRect.height,
                            payload: container.dataset.content,
                            contentlets: contentlets.map((contentlet) => {
                                const contentletRect = contentlet.getBoundingClientRect();

                                return {
                                    x: 0,
                                    y: contentletRect.y - containerRect.y,
                                    width: contentletRect.width,
                                    height: contentletRect.height,
                                    payload: contentlet.dataset.content
                                };
                            })
                        };
                    })
                };
            })
        };
    });
}

export const getPageContainers = (containers) => {
    return Object.keys(containers).reduce((acc, container) => {
        const contentlets = containers[container].contentlets;

        const contentletsKeys = Object.keys(contentlets);

        contentletsKeys.forEach((key) => {
            acc.push({
                identifier:
                    containers[container].container.path ??
                    containers[container].container.identifier,
                uuid: key.replace('uuid-', ''),
                contentletsId: contentlets[key].map((contentlet) => contentlet.identifier)
            });
        });

        return acc;
    }, []);
};

// This extracts all the data we need in the container component
export const getContainersData = (containers, containerRef) => {
    const { identifier, uuid } = containerRef;

    const { containerStructures } = containers[identifier];

    // Get accepts types of content types for this container
    const acceptTypes = containerStructures.map((structure) => structure.contentTypeVar).join(',');

    // Get the contentlets for "this" container
    const contentlets = containers[identifier].contentlets[`uuid-${uuid}`];

    const pageContainers = getPageContainers(containers);

    return {
        ...containers[identifier].container,
        acceptTypes,
        contentlets,
        pageContainers
    };
};
