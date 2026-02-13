import { signalStoreFeature, type, withComputed } from '@ngrx/signals';

import { computed, Signal } from '@angular/core';

import {
    DotCMSLayout,
    DotCMSPage,
    DotCMSPageAssetContainers,
    DotCMSSite,
    DotCMSTemplate,
    DotCMSURLContentMap,
    DotCMSVanityUrl,
    DotCMSViewAs
} from '@dotcms/types';

import { UVEState } from '../models';
import { ClientConfigState } from './client/withClient';

/**
 * Computed signals for accessing pageAsset properties.
 *
 * This feature provides structured access to pageAsset data from the client state.
 * Properties exposed here match the DotCMSPageAsset structure and are guaranteed
 * to stay in sync with the pageAssetResponse state.
 *
 * @remarks
 * All page-related APIs use 'page*' prefix for clear domain ownership and better discoverability.
 * Type store.page to see all page-related APIs grouped together in IntelliSense.
 *
 * @example
 * // ✅ CORRECT: Use domain-prefixed computed signals
 * const page = store.pageData();
 * const site = store.pageSite();
 * const containers = store.pageContainers();
 *
 * @public Shared API - safe for all features to access
 */
export interface PageAssetComputed {
    /**
     * Current page data from pageAsset.
     * Provides access to page properties like title, identifier, canEdit, etc.
     */
    pageData: Signal<DotCMSPage | null>;

    /**
     * Current site data from pageAsset.
     * Provides access to site properties like identifier, hostname, etc.
     */
    pageSite: Signal<DotCMSSite | null>;

    /**
     * Containers structure from pageAsset.
     * Maps container identifiers to their structure and contentlets.
     */
    pageContainers: Signal<DotCMSPageAssetContainers | null>;

    /**
     * Template data from pageAsset.
     * Provides access to template properties like title, layout, drawed, etc.
     */
    pageTemplate: Signal<DotCMSTemplate | Pick<DotCMSTemplate, 'drawed' | 'theme' | 'anonymous' | 'identifier'> | null>;

    /**
     * Layout data from pageAsset.
     * Provides access to layout structure (rows, columns, containers).
     */
    pageLayout: Signal<DotCMSLayout | null>;

    /**
     * ViewAs configuration from pageAsset.
     * Contains language, persona, and visitor context.
     */
    pageViewAs: Signal<DotCMSViewAs | null>;

    /**
     * Vanity URL data from pageAsset.
     * Contains vanity URL configuration if applicable.
     */
    pageVanityUrl: Signal<DotCMSVanityUrl | null>;

    /**
     * URL content map from pageAsset.
     * Maps URL paths to contentlets.
     */
    pageUrlContentMap: Signal<DotCMSURLContentMap | null>;

    /**
     * Number of contentlets on the page.
     * Used for determining if content deletion is allowed.
     */
    pageNumberContents: Signal<number | null>;

    /**
     * Complete client response data.
     * Provides full response including pageAsset, content, and request metadata.
     *
     * Modes:
     * - Legacy mode: pageAsset only (for old clients)
     * - Modern mode: { pageAsset, content, requestMetadata }
     */
    pageClientResponse: Signal<any>;
}

/**
 * A feature that provides computed signals for accessing pageAsset properties.
 *
 * This feature acts as the single source of truth for pageAsset data,
 * ensuring all consumers stay synchronized with the client state.
 *
 * @remarks
 * Phase 6: Centralize pageAsset access through computed signals.
 * This eliminates direct state property access and provides a clear contract
 * for pageAsset data consumption.
 *
 * @export
 */
export function withPageAsset() {
    return signalStoreFeature(
        { state: type<UVEState & ClientConfigState>() },
        withComputed(({ pageAssetResponse, legacyResponseFormat, requestMetadata }) => {
            // ============ PageAsset Properties (Single Source of Truth) ============
            const pageData = computed<DotCMSPage | null>(
                () => pageAssetResponse()?.pageAsset?.page ?? null
            );

            const pageSite = computed<DotCMSSite | null>(
                () => pageAssetResponse()?.pageAsset?.site ?? null
            );

            const pageContainers = computed<DotCMSPageAssetContainers | null>(
                () => pageAssetResponse()?.pageAsset?.containers ?? null
            );

            const pageTemplate = computed<DotCMSTemplate | Pick<DotCMSTemplate, 'drawed' | 'theme' | 'anonymous' | 'identifier'> | null>(
                () => pageAssetResponse()?.pageAsset?.template ?? null
            );

            const pageLayout = computed<DotCMSLayout | null>(
                () => pageAssetResponse()?.pageAsset?.layout ?? null
            );

            const pageViewAs = computed<DotCMSViewAs | null>(
                () => pageAssetResponse()?.pageAsset?.viewAs ?? null
            );

            const pageVanityUrl = computed<DotCMSVanityUrl | null>(
                () => pageAssetResponse()?.pageAsset?.vanityUrl ?? null
            );

            const pageUrlContentMap = computed<DotCMSURLContentMap | null>(
                () => pageAssetResponse()?.pageAsset?.urlContentMap ?? null
            );

            const pageNumberContents = computed<number | null>(
                () => pageAssetResponse()?.pageAsset?.numberContents ?? null
            );

            // ============ Client Response (External Integration) ============
            const pageClientResponse = computed(() => {
                if (!pageAssetResponse()) {
                    return null;
                }

                // Old customers using graphQL expect only the page.
                // We can remove this once we are in stable and tell the devs this won't work in new dotCMS versions.
                if (legacyResponseFormat()) {
                    return pageAssetResponse()?.pageAsset;
                }

                return {
                    ...pageAssetResponse(),
                    requestMetadata: requestMetadata()
                };
            });

            return {
                pageData,
                pageSite,
                pageContainers,
                pageTemplate,
                pageLayout,
                pageViewAs,
                pageVanityUrl,
                pageUrlContentMap,
                pageNumberContents,
                pageClientResponse
            } satisfies PageAssetComputed;
        })
    );
}
