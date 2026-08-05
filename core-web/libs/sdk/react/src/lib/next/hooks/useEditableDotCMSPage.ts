import { useState, useEffect } from 'react';

import {
    DotCMSComposedPageResponse,
    DotCMSPageResponse,
    UVEEventType,
    DotCMSExtendedPageResponse
} from '@dotcms/types';
import { getUVEState, initUVE, createUVESubscription, updateNavigation } from '@dotcms/uve';
import { registerStyleEditorSchemas } from '@dotcms/uve/internal';

/**
 * Custom hook to manage the editable state of a DotCMS page.
 *
 * This hook initializes the Universal Visual Editor (UVE) and subscribes to content changes.
 * It updates the editable page state when content changes are detected in the UVE,
 * ensuring your React components always display the latest content when editing in DotCMS.
 *
 * @example
 * ```ts
 * // Import the hook and the client
 * import { useEditableDotCMSPage } from '@dotcms/react';
 * import { createDotCMSClient } from '@dotcms/client';
 *
 * // Create the client
 * const client = createDotCMSClient({
 *   dotcmsURL: 'https://your-dotcms-instance.com',
 *   authToken: 'your-auth-token'
 * });
 *
 * // Get the page
 * const page = await client.page.get('/', {
 *   languageId: '1',
 * });
 *
 * // Use the hook to get an editable version of the page
 * const editablePage = useEditableDotCMSPage(page);
 *
 * // Then use the page data in your component
 * return (
 *   <div>
 *     <h1>{editablePage.page.title}</h1>
 *     <div dangerouslySetInnerHTML={{ __html: editablePage.page.body }} />
 *   </div>
 * );
 * ```
 *
 * @example
 * ```ts
 * // Import the hook and the client
 * import { useEditableDotCMSPage } from '@dotcms/react';
 * import { createDotCMSClient } from '@dotcms/client';
 *
 * // Create the client
 * const client = createDotCMSClient({
 *   dotcmsURL: 'https://your-dotcms-instance.com',
 *   authToken: 'your-auth-token'
 * });
 *
 * // Get the page with GraphQL content
 * const page = await client.page.get('/', {
 *   languageId: '1',
 *   graphql: {
 *     content: {
 *       products: `ProductCollection(query: "+title:snow", limit: 10, offset: 0, sortBy: "score") {
 *         title
 *         urlMap
 *         category {
 *           name
 *           inode
 *         }
 *         retailPrice
 *         image {
 *           versionPath
 *         }
 *       }`
 *     }
 *   }
 * });
 *
 * // Use the hook to get an editable version of the page and its content
 * const editablePage = useEditableDotCMSPage(page);
 *
 * // Access both page data and GraphQL content
 * const { page: pageData, content } = editablePage;
 *
 * // Use the products from GraphQL content
 * return (
 *   <div>
 *     <h1>{pageData.title}</h1>
 *     <ProductList products={content.products} />
 *   </div>
 * );
 * ```
 * @param {DotCMSPageResponse | Pick<DotCMSPageResponse, 'graphql'>} [pageResponse] - The page data
 * from client.page.get(). If that call threw (draft/non-live page, permissions), pass
 * `{ graphql: error.graphql }` instead — `DotErrorPage` always carries the GraphQL query it
 * attempted, even on failure. Forwarding it here lets the editor retry the fetch with edit-mode
 * permissions and deliver the real content via `postMessage`; pass `undefined` and there's nothing
 * for the editor to retry.
 *
 * @returns {DotCMSPageResponse} The updated editable page state that reflects any changes made in the UVE.
 * The structure includes page data and any GraphQL content that was requested.
 */
export const useEditableDotCMSPage = <T extends DotCMSExtendedPageResponse>(
    pageResponse: DotCMSComposedPageResponse<T> | Pick<DotCMSPageResponse, 'graphql'> | undefined
): DotCMSComposedPageResponse<T> | undefined => {
    const pageData = pageResponse && 'pageAsset' in pageResponse ? pageResponse : undefined;

    const [updatedPageResponse, setUpdatedPageResponse] =
        useState<DotCMSComposedPageResponse<T> | undefined>(pageData);

    useEffect(() => {
        if (!getUVEState()) {
            // Outside UVE, state only ever comes from props - keep it in sync with
            // whatever pageResponse the parent re-renders with (e.g. after a client-side
            // navigation refetches page data), since the initial useState value above is
            // otherwise frozen after the first render.
            setUpdatedPageResponse(pageData);

            return;
        }

        // Inside UVE, pageResponse can be undefined, or just `{ graphql }` (e.g. a draft/non-live
        // page the customer's own fetch treated as a 404, but still knows the GraphQL query it
        // attempted) - forward it to initUVE either way so the editor has something to retry with
        // edit-mode permissions. Without a query to retry, CONTENT_CHANGES never arrives.
        const pageURI = pageData?.pageAsset?.page?.pageURI;

        const { destroyUVESubscriptions } = initUVE(pageResponse);

        // Update the navigation to the pageURI, when we have a pageURI
        // Sometimes the page is null due to permissions, so we don't want to update the navigation
        // And wait for the UVE to resolve the page
        if (pageURI) {
            updateNavigation(pageURI);
        }

        if (pageData?.styleEditorSchemas?.length) {
            registerStyleEditorSchemas(pageData.styleEditorSchemas);
        }

        return () => {
            destroyUVESubscriptions();
        };
    }, [pageResponse, pageData]);

    useEffect(() => {
        const { unsubscribe } = createUVESubscription(
            UVEEventType.CONTENT_CHANGES,
            (payload: DotCMSComposedPageResponse<T>) => {
                setUpdatedPageResponse(payload);
            }
        );

        return () => {
            unsubscribe();
        };
    }, []);

    return updatedPageResponse;
};
