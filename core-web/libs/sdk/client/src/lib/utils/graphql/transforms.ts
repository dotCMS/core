/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    DotCMSBasicContentlet,
    DotCMSGraphQLPageContainer,
    DotCMSGraphQLPageResponse,
    DotCMSPageAssetContainers,
    DotCMSPageContainerContentlets,
    DotCMSPage,
    DotCMSPageAsset,
    DotCMSContainer
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
export const graphqlToPageEntity = (
    graphQLPageResponse: DotCMSGraphQLPageResponse
): DotCMSPageAsset | null => {
    const { page } = graphQLPageResponse;

    // If there is no page, return null
    if (!page) {
        return null;
    }

    const {
        layout,
        template,
        containers,
        urlContentMap,
        viewAs,
        host,
        vanityUrl,
        runningExperimentId,
        _map,
        ...pageAsset
    } = page;
    const data = (_map || {}) as Record<string, unknown>;

    const typedPageAsset = pageAsset as unknown as DotCMSPage;

    // To prevent type errors, we cast the urlContentMap to an object
    const urlContentMapObject = urlContentMap;

    // Extract the _map data from the urlContentMap object
    const urlContentMapData = urlContentMapObject?.['_map'];

    const hostContent = host?.['_map'];

    return {
        layout,
        template,
        viewAs,
        vanityUrl,
        runningExperimentId,
        site: hostContent,
        urlContentMap: urlContentMapData,
        containers: parseContainers(containers as []),
        page: {
            ...data,
            ...typedPageAsset
        }
    };
};

/**
 * Parses the containers from the GraphQL response.
 *
 * @param {DotCMSGraphQLPageContainer[]} [containers=[]] - The containers array from the GraphQL response.
 * @returns {DotCMSPageAssetContainers} The parsed containers.
 */
const parseContainers = (
    containers: DotCMSGraphQLPageContainer[] = []
): DotCMSPageAssetContainers => {
    return containers.reduce(
        (acc: DotCMSPageAssetContainers, container: DotCMSGraphQLPageContainer) => {
            const { path, identifier, containerStructures, containerContentlets, ...rest } =
                container;

            const key = (path || identifier) as string;

            acc[key] = {
                containerStructures,
                container: {
                    path,
                    identifier,
                    ...rest
                } as DotCMSContainer,
                contentlets: parseContentletsToUuidMap(containerContentlets as [])
            };

            return acc;
        },
        {}
    );
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
