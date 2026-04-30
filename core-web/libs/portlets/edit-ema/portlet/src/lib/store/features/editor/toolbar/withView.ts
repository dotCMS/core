import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotDevice, SeoMetaTagsResult } from '@dotcms/dotcms-models';
import { DotCMSURLContentMap, UVE_MODE } from '@dotcms/types';

import {
    DEFAULT_PERSONA,
    MIN_IFRAME_HEIGHT,
    MIN_IFRAME_WIDTH
} from '../../../../shared/consts';
import { InfoOptions } from '../../../../shared/models';
import { getFullPageURL, getIsDefaultVariant, getOrientation } from '../../../../utils';
import { PageComputed } from '../../../features/page/withPage';
import { Orientation, PageType, UVEState } from '../../../models';
import { PersonaSelectorProps } from '../models';

/**
 * Resolve a device + orientation to pixel dimensions for the iframe.
 * Devices store cssWidth/cssHeight in their natural (portrait) orientation;
 * landscape swaps the two.
 */
function getDeviceDimensions(
    device: DotDevice,
    orientation?: Orientation
): { width: number; height: number } {
    const cssW = Number(device?.cssWidth) || 0;
    const cssH = Number(device?.cssHeight) || 0;
    if (orientation === Orientation.LANDSCAPE) {
        return { width: Math.max(cssW, cssH), height: Math.min(cssW, cssH) };
    }
    return { width: Math.min(cssW, cssH), height: Math.max(cssW, cssH) };
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
export function withView() {
    return signalStoreFeature(
        {
            state: type<UVEState>(),
            props: type<PageComputed>()
        },
        withComputed((store) => ({
            viewMode: computed(() => store.pageParams()?.mode ?? UVE_MODE.UNKNOWN),

            $isEditMode: computed(
                () => (store.pageParams()?.mode ?? UVE_MODE.UNKNOWN) === UVE_MODE.EDIT
            ),
            $isPreviewMode: computed(
                () => (store.pageParams()?.mode ?? UVE_MODE.UNKNOWN) === UVE_MODE.PREVIEW
            ),
            $isLiveMode: computed(
                () => (store.pageParams()?.mode ?? UVE_MODE.UNKNOWN) === UVE_MODE.LIVE
            ),

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
                const isEditMode = store.pageParams()?.mode === UVE_MODE.EDIT;

                const viewAs = store.pageAsset()?.viewAs;
                const isDefaultVariant = getIsDefaultVariant(viewAs?.variantId);

                return isEditMode && isDefaultVariant;
            }),

            // Zoom state accessors — viewZoomLevel is stored as integer percentage (10–300);
            // $viewZoomLevel exposes the CSS scale factor (0.1–3.0) for transforms.
            $viewZoomLevel: computed(() => store.viewZoomLevel() / 100),

            // True when no specific device preset is active. In this mode the iframe
            // size tracks the canvas viewport and the user can drag the resize handles.
            $viewIsResponsiveMode: computed(() => {
                const device = store.viewDevice();
                return !device || device.inode === 'default';
            }),

            // Canvas styles
            // Outer is sized to the user-set on-screen dimensions and never
            // changes with zoom (so resize handles stay anchored).
            // Inner is sized to dimensions / zoom and is scaled up by the zoom
            // factor — visually fills the outer box at any zoom, but the page
            // inside the iframe sees a smaller viewport at higher zoom levels.
            $viewCanvasOuterStyles: computed(() => {
                const width = store.viewIframeWidth();
                const height = store.viewIframeHeight();
                return {
                    width: `${width}px`,
                    height: `${height}px`,
                    overflow: 'hidden'
                };
            }),
            $viewCanvasInnerStyles: computed(() => {
                const zoom = store.viewZoomLevel() / 100;
                const width = store.viewIframeWidth();
                const height = store.viewIframeHeight();
                return {
                    width: `${width / zoom}px`,
                    height: `${height / zoom}px`,
                    transform: `scale(${zoom})`,
                    transformOrigin: 'top left'
                };
            })
        })),
        withMethods((store) => ({
            viewSetDevice: (device: DotDevice, orientation?: Orientation) => {
                const isValidOrientation = Object.values(Orientation).includes(orientation);
                const newOrientation = isValidOrientation ? orientation : getOrientation(device);

                const isDefaultDevice = !device || device.inode === 'default';
                const sizePatch: Partial<UVEState> = {};
                if (!isDefaultDevice) {
                    const dims = getDeviceDimensions(device, newOrientation);
                    sizePatch.viewIframeWidth = dims.width;
                    sizePatch.viewIframeHeight = dims.height;
                }

                patchState(store, {
                    ...sizePatch,
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
                const device = store.viewDevice();
                const sizePatch: Partial<UVEState> = {};
                if (device && device.inode !== 'default') {
                    const dims = getDeviceDimensions(device, orientation);
                    sizePatch.viewIframeWidth = dims.width;
                    sizePatch.viewIframeHeight = dims.height;
                }

                patchState(store, {
                    ...sizePatch,
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
             * Set the zoom level (clamped to 10–300). The iframe size is preserved;
             * zooming in past the canvas viewport will cause the canvas to scroll
             * (DevTools behavior).
             */
            viewZoomSetLevel(viewZoomLevel: number): void {
                const clampedZoom = Math.max(10, Math.min(300, viewZoomLevel));
                patchState(store, { viewZoomLevel: clampedZoom });
            },

            /**
             * Increase zoom level by 10% (max 300%).
             */
            viewZoomIn(): void {
                this.viewZoomSetLevel(store.viewZoomLevel() + 10);
            },

            /**
             * Decrease zoom level by 10% (min 10%).
             */
            viewZoomOut(): void {
                this.viewZoomSetLevel(store.viewZoomLevel() - 10);
            },

            /**
             * Reset zoom to 100% and, in responsive mode, snap the iframe back to
             * fit the available canvas viewport. Device mode keeps its preset size.
             */
            viewZoomReset(): void {
                const patch: Partial<UVEState> = { viewZoomLevel: 100 };
                if (isResponsiveMode(store.viewDevice())) {
                    const canvasW = store.viewCanvasAvailableWidth();
                    const canvasH = store.viewCanvasAvailableHeight();
                    if (canvasW > 0 && canvasH > 0) {
                        patch.viewIframeWidth = Math.max(MIN_IFRAME_WIDTH, canvasW);
                        patch.viewIframeHeight = Math.max(MIN_IFRAME_HEIGHT, canvasH);
                    }
                }
                patchState(store, patch);
            },

            /**
             * Get formatted zoom label for display (e.g., "150%").
             */
            viewZoomLabel(): string {
                return `${store.viewZoomLevel()}%`;
            },

            /**
             * Set iframe dimensions. In responsive mode, clamps the *zoomed* size to
             * the available canvas viewport so the iframe never exceeds the visible
             * area (no canvas scrollbars in responsive mode). In device mode, only
             * the MIN_IFRAME_WIDTH / MIN_IFRAME_HEIGHT floor applies.
             */
            viewSetIframeSize(size: { width?: number; height?: number }): void {
                const isResponsive = isResponsiveMode(store.viewDevice());
                const zoom = store.viewZoomLevel() / 100;
                const canvasW = store.viewCanvasAvailableWidth();
                const canvasH = store.viewCanvasAvailableHeight();
                const maxW = isResponsive && canvasW > 0 ? canvasW / zoom : Infinity;
                const maxH = isResponsive && canvasH > 0 ? canvasH / zoom : Infinity;

                const patch: Partial<UVEState> = {};
                if (size.width !== undefined) {
                    patch.viewIframeWidth = clampIframeDim(size.width, MIN_IFRAME_WIDTH, maxW);
                }
                if (size.height !== undefined) {
                    patch.viewIframeHeight = clampIframeDim(size.height, MIN_IFRAME_HEIGHT, maxH);
                }
                patchState(store, patch);
            },

            /**
             * Update the available canvas viewport size. The editor calls this from
             * a ResizeObserver. In responsive mode, the iframe re-clamps so the
             * zoomed dimensions still fit.
             */
            viewSetCanvasAvailableSize(size: { width: number; height: number }): void {
                patchState(store, {
                    viewCanvasAvailableWidth: Math.max(0, Math.round(size.width)),
                    viewCanvasAvailableHeight: Math.max(0, Math.round(size.height))
                });
            }
        }))
    );
}

function clampIframeDim(value: number, min: number, max: number): number {
    return Math.max(min, Math.min(max, Math.round(value)));
}

function isResponsiveMode(device: UVEState['viewDevice']): boolean {
    return !device || device.inode === 'default';
}
