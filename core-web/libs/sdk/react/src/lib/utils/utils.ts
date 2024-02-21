import { ContainerData, PageProviderContext } from '../components/PageProvider/PageProvider';

const endClassMap: Record<number, string> = {
    1: 'col-end-1',
    2: 'col-end-2',
    3: 'col-end-3',
    4: 'col-end-4',
    5: 'col-end-5',
    6: 'col-end-6',
    7: 'col-end-7',
    8: 'col-end-8',
    9: 'col-end-9',
    10: 'col-end-10',
    11: 'col-end-11',
    12: 'col-end-12',
    13: 'col-end-13'
};

const startClassMap: Record<number, string> = {
    1: 'col-start-1',
    2: 'col-start-2',
    3: 'col-start-3',
    4: 'col-start-4',
    5: 'col-start-5',
    6: 'col-start-6',
    7: 'col-start-7',
    8: 'col-start-8',
    9: 'col-start-9',
    10: 'col-start-10',
    11: 'col-start-11',
    12: 'col-start-12'
};

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
                            container.querySelectorAll('[data-dot="contentlet"]')
                        ) as HTMLDivElement[];

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

export const getContainersData = (
    containers: ContainerData,
    containerRef: PageProviderContext['layout']['body']['rows'][0]['columns'][0]['containers'][0]
) => {
    const { identifier, uuid } = containerRef;

    const { containerStructures, container } = containers[identifier];

    // Get the variant id
    const { variantId } = container?.parentPermissionable || {};

    // Get accepts types of content types for this container
    const acceptTypes = containerStructures.map((structure) => structure.contentTypeVar).join(',');

    // Get the contentlets for "this" container
    const contentlets = containers[identifier].contentlets[`uuid-${uuid}`];

    return {
        ...containers[identifier].container,
        acceptTypes,
        contentlets,
        variantId
    };
};

export const combineClasses = (classes: string[]) => classes.filter(Boolean).join(' ');

export const getPositionStyleClasses = (start: number, end: number) => {
    const startClass = startClassMap[start];
    const endClass = endClassMap[end];

    return {
        startClass,
        endClass
    };
};
