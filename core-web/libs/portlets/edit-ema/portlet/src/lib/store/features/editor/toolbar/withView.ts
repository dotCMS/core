import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';

import { computed } from '@angular/core';

import { DotDevice, SeoMetaTagsResult } from '@dotcms/dotcms-models';
import { DotCMSURLContentMap, UVE_MODE } from '@dotcms/types';

import { DEFAULT_PERSONA, MIN_IFRAME_HEIGHT, MIN_IFRAME_WIDTH } from '../../../../shared/consts';
import { EDITOR_STATE } from '../../../../shared/enums';
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
 * - User-pickable presets: 50 / 75 / 100 / 150 / 200 (`dot-uve-zoom-controls`).
 *   The current value is added to the list when a device preset's auto-fit
 *   zoom doesn't match a preset.
 * - Internal clamp: 10–300, wider than the UI presets so device-fit math
 *   (`computeDeviceFit`) can land below 50% on small canvases.
 * - Reset zoom to 100% (and snap iframe to canvas in responsive mode).
 * - `$viewZoomFactor` exposes the value as a 0.1–3.0 multiplier for transform: scale().
 *
 * **Iframe Sizing:**
 * - User-driven width/height via `viewResizeIframe` (size inputs, presets)
 * - Canvas-viewport tracking via `viewSetCanvasAvailableSize` (ResizeObserver)
 * - Device preset fit via `viewSetDevice`
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

            // Zoom factor for transform: scale() — viewZoomLevel is stored as an
            // integer percentage (10–300); display consumers read viewZoomLevel
            // directly.
            $viewZoomFactor: computed(() => store.viewZoomLevel() / 100),

            // True when no specific device preset is active. In this mode the iframe
            // size tracks the canvas viewport and the user can drag the resize handles.
            $viewIsResponsiveMode: computed(() => isResponsiveMode(store.viewDevice())),

            // Canvas styles
            // Outer is sized to the user-set on-screen dimensions and never
            // changes with zoom (so resize handles stay anchored).
            // Inner is sized to dimensions / zoom and is scaled up by the zoom
            // factor — visually fills the outer box at any zoom, but the page
            // inside the iframe sees a smaller viewport at higher zoom levels.
            $viewCanvasOuterStyles: computed(() => {
                const width = store.viewIframeWidth();
                const height = store.viewIframeHeight();
                // Before the iframe/canvas measure (initial 0×0), fall back to
                // 100% so the resize-handles overlay and the canvas-inner have
                // a sensible size to render against. Once the real dimensions
                // arrive, the px values take over.
                return {
                    width: width > 0 ? `${width}px` : '100%',
                    height: height > 0 ? `${height}px` : '100%'
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
                if (isDefaultDevice) {
                    // Default ("Desktop") = responsive: snap to canvas at 100% zoom.
                    const canvasW = store.viewCanvasAvailableWidth();
                    const canvasH = store.viewCanvasAvailableHeight();
                    if (canvasW > 0 && canvasH > 0) {
                        sizePatch.viewIframeWidth = canvasW;
                        sizePatch.viewIframeHeight = canvasH;
                    }
                    sizePatch.viewZoomLevel = 100;
                } else {
                    const dims = getDeviceDimensions(device, newOrientation);
                    const fit = computeDeviceFit(
                        dims,
                        store.viewCanvasAvailableWidth(),
                        store.viewCanvasAvailableHeight()
                    );
                    sizePatch.viewIframeWidth = fit.width;
                    sizePatch.viewIframeHeight = fit.height;
                    sizePatch.viewZoomLevel = fit.zoom;
                }

                patchState(store, {
                    ...sizePatch,
                    viewDevice: device,
                    viewSocialMedia: null,
                    viewDeviceOrientation: newOrientation,
                    viewParams: {
                        ...store.viewParams(),
                        device: device.inode,
                        orientation: newOrientation ?? null,
                        seo: null
                    },
                    // The iframe will reflow at the new dimensions, so any
                    // selected/hover overlay points at stale coordinates.
                    // Flip to RESIZING (hides overlays) and drop the selection;
                    // the SDK's auto-bounds channel will push fresh bounds
                    // once the layout settles, and the SET_BOUNDS handler
                    // flips state back to IDLE.
                    editorSelected: null,
                    editorState: EDITOR_STATE.RESIZING
                });
            },
            /**
             * Exit a device preset without changing the iframe size or zoom.
             * Used when the user starts dragging a resize handle while a device
             * is active — they want to keep editing from the current visual size.
             */
            viewExitDevicePreset: () => {
                if (isResponsiveMode(store.viewDevice())) {
                    return;
                }
                const params = store.viewParams();
                patchState(store, {
                    viewDevice: null,
                    viewDeviceOrientation: null,
                    viewParams: params ? { ...params, device: null, orientation: null } : null
                });
            },
            viewSetOrientation: (orientation: Orientation) => {
                const device = store.viewDevice();
                const sizePatch: Partial<UVEState> = {};
                if (device && device.inode !== 'default') {
                    const dims = getDeviceDimensions(device, orientation);
                    const fit = computeDeviceFit(
                        dims,
                        store.viewCanvasAvailableWidth(),
                        store.viewCanvasAvailableHeight()
                    );
                    sizePatch.viewIframeWidth = fit.width;
                    sizePatch.viewIframeHeight = fit.height;
                    sizePatch.viewZoomLevel = fit.zoom;
                }

                const params = store.viewParams();
                patchState(store, {
                    ...sizePatch,
                    viewDeviceOrientation: orientation,
                    viewParams: params ? { ...params, orientation } : null,
                    editorSelected: null,
                    editorState: EDITOR_STATE.RESIZING
                });
            },
            viewSetSEO: (socialMedia: string | null) => {
                patchState(store, {
                    viewDevice: null,
                    viewDeviceOrientation: null,
                    viewSocialMedia: socialMedia,
                    viewParams: {
                        ...store.viewParams(),
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
                        ...store.viewParams(),
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
             * Set the zoom level. Clamped to 10–300 to accommodate device-fit
             * math; the user-facing zoom controls expose a narrower 50–200
             * preset range plus the current off-preset value (see
             * dot-uve-zoom-controls). Iframe size is preserved; zooming in
             * past the canvas viewport scrolls the canvas (DevTools behavior).
             *
             * Flips editorState to RESIZING so the hover/selected overlays
             * hide during the zoom change. The SDK's auto-bounds
             * ResizeObserver picks up the iframe's internal reflow and pushes
             * fresh SET_BOUNDS, which re-anchors editorSelected and flips
             * state back to IDLE.
             */
            viewZoomSetLevel(viewZoomLevel: number): void {
                const clampedZoom = Math.max(10, Math.min(300, viewZoomLevel));
                patchState(store, {
                    viewZoomLevel: clampedZoom,
                    editorContentArea: null,
                    editorState: EDITOR_STATE.RESIZING
                });
            },

            /**
             * Reset zoom to 100% and, in responsive mode, snap the iframe back to
             * fit the available canvas viewport. Device mode keeps its preset size.
             */
            viewZoomReset(): void {
                const patch: Partial<UVEState> = {
                    viewZoomLevel: 100,
                    editorContentArea: null,
                    editorState: EDITOR_STATE.RESIZING
                };
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
             * Set iframe dimensions (the on-screen size, since zoom adjusts the
             * iframe's internal viewport rather than its on-screen size). In
             * responsive mode, clamps to the available canvas viewport so the
             * iframe never exceeds the visible area. In device mode, only the
             * MIN_IFRAME_WIDTH / MIN_IFRAME_HEIGHT floor applies.
             *
             * Low-level setter — callers that represent a user resize action
             * (typing in the size inputs, clicking a preset) should use
             * {@link viewResizeIframe} so the device preset gets cleared
             * atomically. Use this directly only for non-user paths like the
             * canvas-viewport sync, where exiting a device preset would be wrong.
             */
            viewSetIframeSize(size: { width?: number; height?: number }): void {
                const isResponsive = isResponsiveMode(store.viewDevice());
                const canvasW = store.viewCanvasAvailableWidth();
                const canvasH = store.viewCanvasAvailableHeight();
                const maxW = isResponsive && canvasW > 0 ? canvasW : Infinity;
                const maxH = isResponsive && canvasH > 0 ? canvasH : Infinity;

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
             * User-driven resize: exit any active device preset (size-preserving)
             * and apply the new dimension in one step. Use this from the size
             * inputs and any other UI where the user explicitly chose a size.
             */
            viewResizeIframe(size: { width?: number; height?: number }): void {
                this.viewExitDevicePreset();
                this.viewSetIframeSize(size);
            },

            /**
             * Update the available canvas viewport size. The editor calls this from
             * a ResizeObserver. In responsive mode, the iframe re-clamps so the
             * zoomed dimensions still fit.
             *
             * Flips editorState to RESIZING when the canvas actually changes
             * size *and* a previous size was already known — the iframe is
             * about to reflow, so overlays must hide until SET_BOUNDS settles
             * them. The "previous size > 0" gate skips the first-paint set
             * (`0 → real`), where there's no selection to invalidate yet
             * and flipping RESIZING would freeze a fresh editor unnecessarily.
             */
            viewSetCanvasAvailableSize(size: { width: number; height: number }): void {
                const newWidth = Math.max(0, Math.round(size.width));
                const newHeight = Math.max(0, Math.round(size.height));
                const prevWidth = store.viewCanvasAvailableWidth();
                const prevHeight = store.viewCanvasAvailableHeight();
                const canvasChanged = newWidth !== prevWidth || newHeight !== prevHeight;
                const hadPrev = prevWidth > 0 && prevHeight > 0;

                const patch: Partial<UVEState> = {
                    viewCanvasAvailableWidth: newWidth,
                    viewCanvasAvailableHeight: newHeight
                };
                if (canvasChanged && hadPrev) {
                    patch.editorBounds = [];
                    patch.editorContentArea = null;
                    patch.editorState = EDITOR_STATE.RESIZING;
                }
                patchState(store, patch);
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

/**
 * Fit a device's natural pixel dimensions inside the canvas viewport: pick the
 * largest zoom (≤ 1) that keeps both width and height within the canvas, then
 * return the resulting on-screen size and zoom percentage. Falls back to the
 * device dimensions at 100% zoom when the canvas size is unknown.
 */
function computeDeviceFit(
    deviceDims: { width: number; height: number },
    canvasW: number,
    canvasH: number
): { width: number; height: number; zoom: number } {
    if (canvasW <= 0 || canvasH <= 0 || deviceDims.width <= 0 || deviceDims.height <= 0) {
        return { width: deviceDims.width, height: deviceDims.height, zoom: 100 };
    }
    const rawFit = Math.min(1, canvasW / deviceDims.width, canvasH / deviceDims.height);
    // Clamp to the editor's minimum zoom (10%) and re-derive the on-screen
    // dimensions from the *clamped* fit so the iframe's internal viewport
    // (size / zoom) always matches the device's CSS dimensions exactly.
    // If we kept rawFit for size but used the clamped zoom for transform,
    // the page inside the iframe would see a smaller viewport than the
    // device defines.
    const fit = Math.max(0.1, rawFit);
    return {
        width: Math.round(deviceDims.width * fit),
        height: Math.round(deviceDims.height * fit),
        zoom: Math.round(fit * 100)
    };
}
