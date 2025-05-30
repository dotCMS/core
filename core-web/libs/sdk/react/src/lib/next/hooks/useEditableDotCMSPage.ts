import { useState, useEffect } from 'react';

import {
    DotCMSComposedPageResponse,
    UVEEventType,
    DotCMSExtendedPageResponse
} from '@dotcms/types';
import { getUVEState, initUVE, createUVESubscription, updateNavigation } from '@dotcms/uve';

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
 * @param {DotCMSPageResponse} pageResponse - The initial editable page data from client.page.get().
 *
 * @returns {DotCMSPageResponse} The updated editable page state that reflects any changes made in the UVE.
 * The structure includes page data and any GraphQL content that was requested.
 */
export const useEditableDotCMSPage = <T extends DotCMSExtendedPageResponse>(
    pageResponse: DotCMSComposedPageResponse<T>
): DotCMSComposedPageResponse<T> => {
    const [updatedPageResponse, setUpdatedPageResponse] =
        useState<DotCMSComposedPageResponse<T>>(pageResponse);

    useEffect(() => {
        if (!getUVEState()) {
            return;
        }

        if (!pageResponse) {
            console.warn('[useEditableDotCMSPage]: No DotCMSPageResponse provided');

            return;
        }

        const pageURI = pageResponse?.pageAsset?.page?.pageURI;

        const { destroyUVESubscriptions } = initUVE(pageResponse);

        // Update the navigation to the pageURI, when we have a pageURI
        // Sometimes the page is null due to permissions, so we don't want to update the navigation
        // And wait for the UVE to resolve the page
        if (pageURI) {
            updateNavigation(pageURI);
        }

        return () => {
            destroyUVESubscriptions();
        };
    }, [pageResponse]);

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
