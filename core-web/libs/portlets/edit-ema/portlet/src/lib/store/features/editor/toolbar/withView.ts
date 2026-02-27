import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotDevice, SeoMetaTagsResult } from '@dotcms/dotcms-models';
import { DotCMSURLContentMap, UVE_MODE } from '@dotcms/types';

import { DEFAULT_PERSONA } from '../../../../shared/consts';
import { InfoOptions } from '../../../../shared/models';
import { getFullPageURL, getIsDefaultVariant, getOrientation } from '../../../../utils';
import { PageComputed } from '../../../features/page/withPage';
import { Orientation, PageType, UVEState } from '../../../models';
import { PersonaSelectorProps } from '../models';

/**
 * Dependencies interface for withView
 * Currently no external dependencies needed - all accessed from store
 * Kept for future extensibility
 */
// eslint-disable-next-line @typescript-eslint/no-empty-interface, @typescript-eslint/no-empty-object-type
export interface WithViewDeps {
    // No dependencies needed - all accessed from store
}

/**
 * View feature for UVE store - manages editor view modes, preview configuration, and zoom controls.
 *
 * This feature consolidates all view-related concerns in one place:
 *
 * **Device Preview:**
 * - Select device for responsive preview (mobile, tablet, desktop)
 * - Control device orientation (portrait/landscape)
 * - Clear device preview to return to edit mode
 *
 * **SEO/Social Media Preview:**
 * - Preview how content appears on social platforms (Facebook, Twitter, LinkedIn)
 * - View Open Graph tags and metadata
 *
 * **Zoom Controls:**
 * - Zoom in/out of the editor canvas (0.1x to 3.0x)
 * - Reset zoom to 100%
 * - Support for trackpad pinch gestures
 * - Dynamic canvas dimensions based on iframe height
 *
 * **View Mode:**
 * - Access current mode via `viewMode()` computed signal (EDIT/PREVIEW/LIVE)
 * - No redundant boolean flags - use mode enum for clarity
 *
 * All state follows flat structure with `view*` prefix (e.g., viewDevice, viewZoomLevel).
 *
 * @returns Signal store feature with view management capabilities
 */
export function withView(_deps?: WithViewDeps) {
    return signalStoreFeature(
        {
            state: type<UVEState>(),
            props: type<PageComputed>()
        },
        withComputed((store) => ({
            viewMode: computed(() => store.pageParams()?.mode ?? UVE_MODE.UNKNOWN),

            $urlContentMap: computed<DotCMSURLContentMap | null>(() => {
                const urlContentMap = store.pageAsset()?.urlContentMap;
                // GQL may return an empty object; treat it as null when there are no entries
                if (!urlContentMap || Object.keys(urlContentMap).length === 0) return null;
                return urlContentMap;
            }),
            $personaSelector: computed<PersonaSelectorProps>(() => {
                const page = store.pageAsset()?.page;
                const viewAs = store.pageAsset()?.viewAs;

                return {
                    pageId: page?.identifier,
                    value: viewAs?.persona ?? DEFAULT_PERSONA
                };
            }),
            $apiURL: computed<string>(() => {
                const params = store.pageParams();
                const pageURL = getFullPageURL({ url: params.url, params });

                const apiPageType = store.pageType() === PageType.TRADITIONAL ? 'render' : 'json';
                const pageAPI = `/api/v1/page/${apiPageType}/${pageURL}`;

                return pageAPI;
            }),
            $infoDisplayProps: computed<InfoOptions>(() => {
                const viewAs = store.pageAsset()?.viewAs;
                const mode = store.pageParams()?.mode;

                if (!getIsDefaultVariant(viewAs?.variantId)) {
                    const variantId = viewAs.variantId;

                    const currentExperiment = store.pageExperiment?.();

                    const name =
                        currentExperiment?.trafficProportion.variants.find(
                            (variant) => variant.id === variantId
                        )?.name ?? 'Unknown Variant';

                    // Now we base on the mode to show the correct message
                    const message =
                        mode === UVE_MODE.PREVIEW || mode === UVE_MODE.LIVE
                            ? 'editpage.viewing.variant'
                            : 'editpage.editing.variant';

                    return {
                        info: {
                            message,
                            args: [name]
                        },
                        icon: 'pi pi-file-edit',
                        id: 'variant',
                        actionIcon: 'pi pi-arrow-left'
                    };
                }

                return null;
            }),
            $showWorkflowsActions: computed<boolean>(() => {
                const isPreviewMode = store.pageParams()?.mode === UVE_MODE.PREVIEW;
                const isLiveMode = store.pageParams()?.mode === UVE_MODE.LIVE;

                const viewAs = store.pageAsset()?.viewAs;
                const isDefaultVariant = getIsDefaultVariant(viewAs?.variantId);

                return !isPreviewMode && !isLiveMode && isDefaultVariant;
            }),

            // Zoom state accessors
            $viewZoomLevel: computed(() => store.viewZoomLevel()),
            $viewZoomIsActive: computed(() => store.viewZoomIsActive()),
            $viewIframeDocHeight: computed(() => store.viewZoomIframeDocHeight()),

            // Canvas styles for zoom transform
            $viewCanvasOuterStyles: computed(() => {
                const zoom = store.viewZoomLevel();
                const height = store.viewZoomIframeDocHeight() || 800;
                return {
                    width: `${1520 * zoom}px`,
                    height: `${height * zoom}px`
                };
            }),
            $viewCanvasInnerStyles: computed(() => {
                const zoom = store.viewZoomLevel();
                const height = store.viewZoomIframeDocHeight() || 800;
                return {
                    width: `1520px`,
                    height: `${height}px`,
                    transform: `scale(${zoom})`,
                    transformOrigin: 'top left'
                };
            })
        })),
        withMethods((store) => ({
            viewSetDevice: (device: DotDevice, orientation?: Orientation) => {
                const isValidOrientation = Object.values(Orientation).includes(orientation);
                const newOrientation = isValidOrientation ? orientation : getOrientation(device);

                patchState(store, {
                    viewDevice: device,
                    viewSocialMedia: null,
                    viewDeviceOrientation: newOrientation,
                    viewParams: {
                        ...(store.viewParams() || {}),
                        device: device.inode,
                        orientation: newOrientation,
                        seo: null
                    }
                });
            },
            viewSetOrientation: (orientation: Orientation) => {
                patchState(store, {
                    viewDeviceOrientation: orientation,
                    viewParams: store.viewParams()
                        ? {
                              ...store.viewParams(),
                              orientation
                          }
                        : store.viewParams()
                });
            },
            viewSetSEO: (socialMedia: string | null) => {
                patchState(store, {
                    viewDevice: null,
                    viewDeviceOrientation: null,
                    viewSocialMedia: socialMedia,
                    viewParams: {
                        ...(store.viewParams() || {}),
                        device: null,
                        orientation: null,
                        seo: socialMedia
                    }
                });
            },
            viewClearDeviceAndSocialMedia: () => {
                patchState(store, {
                    viewDevice: null,
                    viewSocialMedia: null,
                    viewDeviceOrientation: null,
                    viewParams: {
                        ...(store.viewParams() || {}),
                        device: null,
                        orientation: null,
                        seo: null
                    }
                });
            },
            viewSetOGTagResults: (ogTagsResults: SeoMetaTagsResult[]) => {
                patchState(store, {
                    viewOgTagsResults: ogTagsResults
                });
            },

            // Zoom methods
            /**
             * Increase zoom level by 0.1 (max 3.0)
             */
            viewZoomIn(): void {
                const currentZoom = store.viewZoomLevel();
                const newZoom = Math.max(0.1, Math.min(3, currentZoom + 0.1));
                patchState(store, { viewZoomLevel: newZoom });
            },

            /**
             * Decrease zoom level by 0.1 (min 0.1)
             */
            viewZoomOut(): void {
                const currentZoom = store.viewZoomLevel();
                const newZoom = Math.max(0.1, Math.min(3, currentZoom - 0.1));
                patchState(store, { viewZoomLevel: newZoom });
            },

            /**
             * Reset zoom level to 1.0 (100%)
             */
            viewZoomReset(): void {
                patchState(store, { viewZoomLevel: 1 });
            },

            /**
             * Get formatted zoom label for display (e.g., "150%")
             */
            viewZoomLabel(): string {
                return `${Math.round(store.viewZoomLevel() * 100)}%`;
            },

            /**
             * Set zoom level directly (clamped between 0.1 and 3.0)
             */
            viewZoomSetLevel(viewZoomLevel: number): void {
                const clampedZoom = Math.max(0.1, Math.min(3, viewZoomLevel));
                patchState(store, { viewZoomLevel: clampedZoom });
            },

            /**
             * Set zoom mode state (active during zoom gestures)
             */
            viewZoomSetActive(viewZoomIsActive: boolean): void {
                patchState(store, { viewZoomIsActive });
            },

            /**
             * Set iframe document height for canvas calculations
             */
            viewSetIframeDocHeight(height: number): void {
                patchState(store, { viewZoomIframeDocHeight: height });
            },

            /**
             * Set gesture start zoom level (for trackpad pinch gestures)
             */
            viewZoomSetGestureStart(zoom: number): void {
                patchState(store, { viewZoomGestureStartZoom: zoom });
            },

            /**
             * Get current gesture start zoom level
             */
            viewZoomGetGestureStart(): number {
                return store.viewZoomGestureStartZoom();
            }
        }))
    );
}
