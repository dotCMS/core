/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    DotCMSBasicContentlet,
    DotCMSGraphQLPageContainer,
    DotCMSGraphQLPage,
    DotCMSPageAssetContainers,
    DotCMSPageContainerContentlets,
    DotCMSPage,
    DotCMSPageAsset,
    DotCMSContainer,
    DotCMSURLContentMap
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
export const graphqlToPageEntity = (page: DotCMSGraphQLPage): DotCMSPageAsset | null => {
    // If there is no page, return null
    if (!page || (typeof page === 'object' && Object.keys(page).length === 0)) {
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
        numberContents,
        _map,
        ...pageAsset
    } = page;
    const data = (_map || {}) as Record<string, unknown>;

    // styleEditorSchemas comes back as null from GraphQL outside EDIT_MODE. Separate it from the
    // rest of the page fields so it can be omitted entirely when it has no value. Emitting
    // `undefined` (the previous behaviour) breaks JSON serialization for consumers like Next.js
    // Pages Router (getServerSideProps/getStaticProps), while omitting the key keeps the optional
    // DotCMSPage.styleEditorSchemas type accurate.
    const { styleEditorSchemas, ...typedPageAsset } = pageAsset as unknown as DotCMSPage;

    // Merge all urlContentMap keys into _map, except _map itself
    const mergedUrlContentMap = {
        ...(urlContentMap?._map || {}),
        ...Object.entries(urlContentMap || {}).reduce<Record<string, unknown>>(
            (acc, [key, value]) => {
                if (key !== '_map') {
                    acc[key] = value;
                }
                return acc;
            },
            {}
        )
    } as DotCMSURLContentMap;

    return {
        layout,
        template,
        viewAs,
        numberContents,
        vanityUrl,
        runningExperimentId,
        site: host,
        urlContentMap: mergedUrlContentMap,
        containers: parseContainers(containers as []),
        page: {
            ...data,
            ...typedPageAsset,
            // Only re-add styleEditorSchemas when it actually has a value (see destructure above).
            ...(styleEditorSchemas ? { styleEditorSchemas } : {})
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

            const key = path || identifier;

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
