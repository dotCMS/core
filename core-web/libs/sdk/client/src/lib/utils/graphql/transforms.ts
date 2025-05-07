/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    DotCMSBasicContentlet,
    DotCMSPageContainerContentlets,
    DotCMSPageContainer,
    DotCMSPageResponse
} from '@dotcms/types';

/**
 * Transforms a GraphQL Page response to a Page Entity.
 *
 * @param {GraphQLPageResponse} graphQLPageResponse - The GraphQL Page response object.
 * @returns {object|null} The transformed Page Entity or null if the page is not present.
 *
 * @example
 * ```ts
 * const pageEntity = graphqlToPageEntity(graphQLPageResponse);
 * ```
 */
export const graphqlToPageEntity = (graphQLPageResponse: DotCMSPageResponse) => {
    const { page } = graphQLPageResponse;

    // If there is no page, return null
    if (!page) {
        return null;
    }

    const { layout, template, containers, urlContentMap, viewAs, site, _map, ...pageAsset } = page;
    const data = (_map || {}) as Record<string, unknown>;

    // To prevent type errors, we cast the urlContentMap to an object
    const urlContentMapObject = urlContentMap as Record<string, unknown>;

    // Extract the _map data from the urlContentMap object
    const urlContentMapData = urlContentMapObject?.['_map'] as Record<string, unknown>;

    return {
        layout,
        template,
        viewAs,
        site,
        page: {
            ...data,
            ...pageAsset
        },
        containers: parseContainers(containers as []),
        urlContentMap: urlContentMapData
    } as any;
};

/**
 * Parses the containers from the GraphQL response.
 *
 * @param {Array<Record<string, unknown>>} [containers=[]] - The containers array from the GraphQL response.
 * @returns {Record<string, unknown>} The parsed containers.
 */
const parseContainers = (containers: DotCMSPageContainer[] = []) => {
    return containers.reduce((acc: Record<string, unknown>, container: DotCMSPageContainer) => {
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

/**
 * Parses the contentlets from the GraphQL response.
 *
 * @param {Array<Record<string, unknown>>} containerContentlets - The contentlets array from the GraphQL response.
 * @returns {Record<string, Array<Record<string, unknown>>>} The parsed contentlets mapped by UUID.
 */
const parseContentletsToUuidMap = (containerContentlets: DotCMSPageContainerContentlets[] = []) => {
    return containerContentlets.reduce(
        (acc, containerContentlet) => {
            const { uuid, contentlets } = containerContentlet;

            // TODO: This is a temporary solution, we need to find a better way to handle this.
            acc[uuid] = contentlets.map(({ _map = {}, ...rest }) => {
                return {
                    ...(_map as Record<string, unknown>),
                    ...rest
                };
            });

            return acc;
        },
        {} as Record<string, DotCMSBasicContentlet[]>
    );
};
