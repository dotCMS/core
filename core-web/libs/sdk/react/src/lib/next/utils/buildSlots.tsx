import { ReactNode } from 'react';

import { DotCMSBasicContentlet, DotCMSPageAssetContainers } from '@dotcms/types';

/**
 * Builds a slots map of pre-rendered server component nodes keyed by contentlet identifier.
 *
 * Use this in Next.js server components to render async server components
 * (e.g., components that fetch data) within a DotCMS page layout. Pass the
 * resulting map to `DotCMSLayoutBody` via the `slots` prop.
 *
 * @public
 * @param containers - The containers map from `pageAsset.containers`
 * @param serverComponents - A map of content type names to async server components
 * @returns A record mapping contentlet identifiers to pre-rendered ReactNodes
 *
 * @example
 * ```tsx
 * const slots = buildSlots(pageContent.pageAsset.containers, {
 *   BlogList: BlogListContainer,
 * });
 *
 * <DotCMSLayoutBody page={pageAsset} components={pageComponents} slots={slots} />
 * ```
 */
export function buildSlots(
    containers: DotCMSPageAssetContainers,
    serverComponents: Record<
        string,
        (props: DotCMSBasicContentlet) => ReactNode | Promise<ReactNode>
    >
): Record<string, ReactNode | Promise<ReactNode>> {
    const slots: Record<string, ReactNode | Promise<ReactNode>> = {};

    for (const { contentlets } of Object.values(containers)) {
        for (const contentletList of Object.values(contentlets)) {
            for (const contentlet of contentletList) {
                const Component = serverComponents[contentlet.contentType];

                if (Component && contentlet.identifier) {
                    slots[contentlet.identifier] = Component(contentlet as DotCMSBasicContentlet);
                }
            }
        }
    }

    return slots;
}
