export const graphqlToPageEntity = ({ page }: { page: Record<string, unknown> }) => {
    const { layout, template, containers, viewAs, ...pageAsset } = page;

    return {
        layout,
        template,
        viewAs,
        page: pageAsset,
        containers: parseContainers(containers as [])
    };
};

const parseContainers = (containers = []) => {
    return containers.reduce((acc: Record<string, unknown>, container: Record<string, unknown>) => {
        const { path, identifier, containerStructures, containerContentlets, ...rest } = container;

        const key = (path || identifier) as string;

        acc[key] = {
            containerStructures,
            container: {
                path,
                identifier,
                ...rest
            },
            contentlets: parseContentletsToUuidMap(containerContentlets as [])
        };

        return acc;
    }, {});
};

const parseContentletsToUuidMap = (contentlets = []) => {
    return contentlets.reduce((acc, contentlet) => {
        const { uuid, contentlets: innerContentlets } = contentlet;

        acc[uuid] = innerContentlets;

        return acc;
    }, {});
};
