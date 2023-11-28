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

    const contentletsId = contentlets.map((contentlet) => contentlet.identifier);

    const pageContainers = getPageContainers(containers);

    return {
        ...containers[identifier],
        acceptTypes,
        contentlets,
        contentletsId,
        pageContainers
    };
};
