// For now, we are using this file to parse the response from the GraphQL API.
/* eslint-disable @typescript-eslint/no-explicit-any */
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

const parseContentletsToUuidMap = (containerContentlets: any[]) => {
    return containerContentlets.reduce((acc, containerContentlet) => {
        const { uuid, contentlets } = containerContentlet;

        acc[uuid] = contentlets.map(({ _map = {}, ...rest }) => {
            return {
                ..._map,
                ...rest
            };
        });

        return acc;
    }, {} as any);
};
