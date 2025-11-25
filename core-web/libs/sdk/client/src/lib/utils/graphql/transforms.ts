/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * Represents the response from a GraphQL query for a page.
 *
 * @interface GraphQLPageResponse
 * @property {Record<string, unknown>} page - The main page data.
 * @property {unknown} [key: string] - Additional properties that may be included in the response.
 */
interface GraphQLPageResponse {
    page: Record<string, unknown>;
    [key: string]: unknown;
}

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
export const graphqlToPageEntity = (graphQLPageResponse: GraphQLPageResponse) => {
    const { page } = graphQLPageResponse;

    // If there is no page, return null
    if (!page) {
        return null;
    }

    const { layout, template, containers, urlContentMap, viewAs, site, _map, ...pageAsset } = page;
    const data = (_map || {}) as Record<string, unknown>;

    return {
        layout,
        template,
        viewAs,
        urlContentMap,
        site,
        page: {
            ...data,
            ...pageAsset
        },
        containers: parseContainers(containers as [])
    } as any;
};

/**
 * Parses the containers from the GraphQL response.
 *
 * @param {Array<Record<string, unknown>>} [containers=[]] - The containers array from the GraphQL response.
 * @returns {Record<string, unknown>} The parsed containers.
 */
const parseContainers = (containers: Record<string, unknown>[] = []) => {
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

/**
 * Parses the contentlets from the GraphQL response.
 *
 * @param {Array<Record<string, unknown>>} containerContentlets - The contentlets array from the GraphQL response.
 * @returns {Record<string, Array<Record<string, unknown>>>} The parsed contentlets mapped by UUID.
 */
const parseContentletsToUuidMap = (containerContentlets: Record<string, unknown>[] = []) => {
    return containerContentlets.reduce((acc, containerContentlet) => {
        const { uuid, contentlets } = containerContentlet as {
            uuid: string;
            contentlets: Record<string, unknown>[];
        };

        // TODO: This is a temporary solution, we need to find a better way to handle this.
        acc[uuid] = contentlets.map(({ _map = {}, ...rest }) => {
            return {
                ...(_map as Record<string, unknown>),
                ...rest
            };
        });

        return acc;
    }, {});
};
