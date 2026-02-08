import {
    DotCMSBasicContentlet,
    DotCMSPageAsset,
    DotCMSPageResponse,
    StyleEditorProperties
} from '@dotcms/types';

import { ActionPayload } from '../../../../../../shared/models';

/**
 * Type representing a GraphQL response that can be either:
 * - Direct DotCMSPageAsset
 * - Wrapped response with pageAsset property
 */
export type GraphQLResponse =
    | DotCMSPageAsset
    | {
          graphql?: DotCMSPageResponse['graphql'];
          pageAsset: DotCMSPageAsset;
          content?: Record<string, unknown>;
      };

/**
 * Extracts the pageAsset from a GraphQL response, handling both wrapped and unwrapped formats
 */
function extractPageAsset(response: GraphQLResponse): DotCMSPageAsset {
    return 'pageAsset' in response ? response.pageAsset : response;
}

/**
 * Updates style properties in a GraphQL response for a specific contentlet.
 * Mutates the response in place and returns it.
 *
 * @param graphqlResponse - The graphql response to update
 * @param payload - The action payload containing container and contentlet info
 * @param styleProperties - The style properties to apply
 * @returns The updated graphql response (same reference, mutated)
 */
export function updateStylePropertiesInGraphQL(
    graphqlResponse: GraphQLResponse,
    payload: ActionPayload,
    styleProperties: StyleEditorProperties
): GraphQLResponse {
    const pageAsset = extractPageAsset(graphqlResponse);
    const containerId = payload.container.identifier;
    const contentletId = payload.contentlet.identifier;
    const uuid = payload.container.uuid;

    const container = pageAsset.containers[containerId];

    if (!container) {
        console.error(`Container with id ${containerId} not found`);
        return graphqlResponse;
    }

    const contentlets = container.contentlets[`uuid-${uuid}`];

    if (!contentlets) {
        console.error(`Contentlet with uuid ${uuid} not found`);
        return graphqlResponse;
    }

    contentlets.forEach((contentlet: DotCMSBasicContentlet) => {
        if (contentlet?.identifier === contentletId) {
            contentlet.dotStyleProperties = styleProperties;
        }
    });

    return graphqlResponse;
}

/**
 * Extracts style properties from a GraphQL response for a specific contentlet.
 * Reverse operation of updateStylePropertiesInGraphQL.
 *
 * @param graphqlResponse - The graphql response to extract from
 * @param payload - The action payload containing container and contentlet info
 * @returns The style properties object or null if not found
 */
export function extractStylePropertiesFromGraphQL(
    graphqlResponse: GraphQLResponse,
    payload: ActionPayload
): StyleEditorProperties | null {
    const pageAsset = extractPageAsset(graphqlResponse);
    const containerId = payload.container.identifier;
    const contentletId = payload.contentlet.identifier;
    const uuid = payload.container.uuid;

    const container = pageAsset.containers[containerId];

    if (!container) {
        return null;
    }

    const contentlets = container.contentlets[`uuid-${uuid}`];

    if (!contentlets) {
        return null;
    }

    const contentlet = contentlets.find(
        (c: DotCMSBasicContentlet) => c?.identifier === contentletId
    );

    return contentlet?.dotStyleProperties || null;
}
