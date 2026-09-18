import { DotCMSBasicContentlet, DotCMSPageAsset } from '@dotcms/types';
import { getContentletsInContainer } from '@dotcms/uve/internal';

import { ContentletActionPayload } from '../../../../shared/models';

/**
 * Filters out null and undefined values from an object recursively.
 *
 * Recursively processes nested objects (like checkbox groups) to remove null/undefined values.
 * Preserves valid values including false, 0, empty strings, and empty arrays as they are
 * meaningful values in form contexts.
 *
 * @param obj - The object to filter
 * @returns A new object with null/undefined values removed, maintaining the structure of nested objects
 *
 * @example
 * ```typescript
 * const filtered = filterFormValues({
 *   name: 'John',
 *   age: null,
 *   active: false,
 *   tags: { tag1: true, tag2: null }
 * });
 * // Returns: { name: 'John', active: false, tags: { tag1: true } }
 * ```
 */
export function filterFormValues<T extends Record<string, unknown>>(obj: T): Partial<T> {
    const filtered: Partial<T> = {};

    for (const [key, value] of Object.entries(obj)) {
        // Skip null and undefined values (but keep empty strings, false, 0, etc.)
        if (value === null || value === undefined) {
            continue;
        }

        // Handle nested objects (like checkbox groups)
        if (typeof value === 'object' && !Array.isArray(value) && value !== null) {
            const filteredNested = filterFormValues(value as Record<string, unknown>);
            // Only include nested object if it has at least one property
            // (empty objects are filtered out)
            if (Object.keys(filteredNested).length > 0) {
                filtered[key as keyof T] = filteredNested as T[keyof T];
            }
        } else {
            // Include non-null, non-undefined primitive values and arrays
            // This includes false, 0, empty strings, etc. as they are valid values
            filtered[key as keyof T] = value as T[keyof T];
        }
    }

    return filtered;
}

/**
 * Updates contentlet properties in a PageAsset for a specific contentlet.
 * Mutates the response in place and returns it.
 *
 * @param pageAsset - The page asset to update
 * @param payload - The action payload containing container and contentlet info
 * @param properties - The properties to apply
 * @returns The updated PageAsset (same reference, mutated)
 */
export function updateContentletPropertiesInPageAsset(
    pageAsset: DotCMSPageAsset,
    payload: ContentletActionPayload,
    properties: Record<string, unknown>
): DotCMSPageAsset {
    const contentletId = payload.contentlet.identifier;

    const contentlets = getContentletsInContainer(pageAsset, {
        identifier: payload.container.identifier,
        uuid: payload.container.uuid,
        historyUUIDs: []
    });

    contentlets.forEach((contentlet: DotCMSBasicContentlet) => {
        if (contentlet?.identifier === contentletId) {
            // `Object.assign` copies exactly the keys the loop copied, without indexing a type that
            // names only the fields every contentlet has. The properties written here are the
            // content type's own — style properties in practice — which is precisely the set
            // `DotCMSBasicContentlet` does not declare.
            Object.assign(contentlet, properties);
        }
    });

    return pageAsset;
}

/**
 * Extracts contentlet properties from a PageAsset for a specific contentlet.
 * Reverse operation of updateContentletPropertiesInPageAsset.
 *
 * @param pageAsset - The page asset to extract from
 * @param payload - The action payload containing container and contentlet info
 * @returns The contentlet properties object or null if not found
 */
export function extractContentletPropertiesFromPageAsset(
    pageAsset: DotCMSPageAsset,
    payload: ContentletActionPayload,
    properties: Array<string>
): Record<string, unknown> {
    const contentletId = payload.contentlet.identifier;

    const contentlets = getContentletsInContainer(pageAsset, {
        identifier: payload.container.identifier,
        uuid: payload.container.uuid,
        historyUUIDs: []
    });

    const contentlet = contentlets.find(
        (c: DotCMSBasicContentlet) => c?.identifier === contentletId
    );

    // Reading a content field by name off a type that declares only the static ones needs the
    // wider view; it is the read half of the `Object.assign` above. Two steps because
    // `DotCMSBasicContentlet` has no index signature to overlap with `Record`, which is the very
    // reason the direct index was an error.
    const contentletFields = contentlet as unknown as Record<string, unknown> | undefined;

    return properties.reduce<Record<string, unknown>>((acc, property) => {
        acc[property] = contentletFields?.[property];

        return acc;
    }, {});
}
