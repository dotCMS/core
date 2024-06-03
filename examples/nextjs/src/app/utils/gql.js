// SDK CLIENT

export const graphqlToPageEntity = ({ page }) => {
    const { layout, template, containers, viewAs, ...pageAsset } = page;
    return {
        layout,
        template,
        viewAs,
        page: {
            ...pageAsset,
        },
        containers: parseContainer(containers),
    };
};

const parseContainer = (containers = []) => {
    return containers.reduce((acc, container) => {
        const {
            path,
            identifier,
            containerStructures,
            containerContentlets,
            ...rest
        } = container;

        const key = path || identifier;

        acc[key] = {
            containerStructures,
            container: {
                path,
                identifier,
                ...rest,
            },
            contentlets: parseContentlets(containerContentlets),
        };

        return acc;
    }, {});
};

const parseContentlets = (contentlets = []) => {
    return contentlets.reduce((acc, contentlet) => {
        const { uuid, contentlets: innerContentlets } = contentlet;

        acc[uuid] = innerContentlets;

        return acc;
    }, {});
};
