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
