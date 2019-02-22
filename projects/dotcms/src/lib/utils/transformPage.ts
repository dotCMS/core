// tslint:disable-next-line:cyclomatic-complexity
export const transformPage = (page) => {
    if (page.layout) {
        page.layout.body.rows.forEach((row) => {
            row.columns.forEach((col) => {
                col.containers = col.containers.map((container) => {
                    return {
                        ...container,
                        ...page.containers[container.identifier].container,
                        acceptTypes: page.containers[container.identifier].containerStructures
                            .map((structure) => structure.contentTypeVar)
                            .join(','),
                        contentlets:
                            page.containers[container.identifier].contentlets[
                                `uuid-${container.uuid}`
                            ]
                    };
                });
            });
        });

        if (
            page.layout.sidebar &&
            page.layout.sidebar.containers &&
            page.layout.sidebar.containers.length
        ) {
            page.layout.sidebar.containers = page.layout.sidebar.containers.map((container) => {
                const contentlets =
                    page.containers[container.identifier].contentlets[`uuid-${container.uuid}`];
                return {
                    ...container,
                    contentlets
                };
            });
        }
    }

    return page;
};
