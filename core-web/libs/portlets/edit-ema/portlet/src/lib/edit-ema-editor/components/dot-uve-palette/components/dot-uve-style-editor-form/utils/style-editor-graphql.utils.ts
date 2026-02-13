import {
    DotCMSBasicContentlet,
    DotCMSPageAsset,
    StyleEditorProperties
} from '@dotcms/types';

import { ActionPayload } from '../../../../../../shared/models';

/**
 * Updates style properties in a GraphQL response for a specific contentlet.
 * Mutates the response in place and returns it.
 *
 * @param pageAsset - The page asset to update
 * @param payload - The action payload containing container and contentlet info
 * @param styleProperties - The style properties to apply
 * @returns The updated graphql response (same reference, mutated)
 */
export function updateStylePropertiesInGraphQL(
    pageAsset: DotCMSPageAsset,
    payload: ActionPayload,
    styleProperties: StyleEditorProperties
): DotCMSPageAsset {
    const containerId = payload.container.identifier;
    const contentletId = payload.contentlet.identifier;
    const uuid = payload.container.uuid;

    const container = pageAsset.containers[containerId];

    if (!container) {
        console.error(`Container with id ${containerId} not found`);
        return pageAsset;
    }

    const contentlets = container.contentlets[`uuid-${uuid}`];

    if (!contentlets) {
        console.error(`Contentlet with uuid ${uuid} not found`);
        return pageAsset;
    }

    contentlets.forEach((contentlet: DotCMSBasicContentlet) => {
        if (contentlet?.identifier === contentletId) {
            contentlet.dotStyleProperties = styleProperties;
        }
    });

    return pageAsset;
}

/**
 * Extracts style properties from a GraphQL response for a specific contentlet.
 * Reverse operation of updateStylePropertiesInGraphQL.
 *
 * @param pageAsset - The page asset to extract from
 * @param payload - The action payload containing container and contentlet info
 * @returns The style properties object or null if not found
 */
export function extractStylePropertiesFromGraphQL(
    pageAsset: DotCMSPageAsset,
    payload: ActionPayload
): StyleEditorProperties | null {
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
