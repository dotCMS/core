import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { ZoomCanvasStyles, ZoomState } from './models';

import { UVEState } from '../../models';

const initialZoomState: ZoomState = {
    zoomLevel: 1,
    isZoomMode: false,
    iframeDocHeight: 0,
    gestureStartZoom: 1
};

/**
 * Zoom feature for the UVE store
 * Manages zoom level, zoom mode, and canvas dimensions for the editor
 *
 * @export
 * @return {*}
 */
export function withZoom() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<{ zoom: ZoomState }>({
            zoom: initialZoomState
        }),
        withComputed((store) => ({
            $canvasOuterStyles: computed<ZoomCanvasStyles['outer']>(() => {
                const zoom = store.zoom().zoomLevel;
                const height = store.zoom().iframeDocHeight || 800;
                return {
                    width: `${1520 * zoom}px`,
                    height: `${height * zoom}px`
                };
            }),
            $canvasInnerStyles: computed<ZoomCanvasStyles['inner']>(() => {
                const zoom = store.zoom().zoomLevel;
                const height = store.zoom().iframeDocHeight || 800;
                return {
                    width: `1520px`,
                    height: `${height}px`,
                    transform: `scale(${zoom})`,
                    transformOrigin: 'top left'
                };
            }),
            $zoomLevel: computed<number>(() => store.zoom().zoomLevel),
            $isZoomMode: computed<boolean>(() => store.zoom().isZoomMode),
            $iframeDocHeight: computed<number>(() => store.zoom().iframeDocHeight)
        })),
        withMethods((store) => {
            return {
                /**
                 * Increase zoom level by 0.1 (max 3.0)
                 */
                zoomIn(): void {
                    const currentZoom = store.zoom().zoomLevel;
                    const newZoom = Math.max(0.1, Math.min(3, currentZoom + 0.1));
                    patchState(store, {
                        zoom: {
                            ...store.zoom(),
                            zoomLevel: newZoom
                        }
                    });
                },

                /**
                 * Decrease zoom level by 0.1 (min 0.1)
                 */
                zoomOut(): void {
                    const currentZoom = store.zoom().zoomLevel;
                    const newZoom = Math.max(0.1, Math.min(3, currentZoom - 0.1));
                    patchState(store, {
                        zoom: {
                            ...store.zoom(),
                            zoomLevel: newZoom
                        }
                    });
                },

                /**
                 * Reset zoom level to 1.0 (100%)
                 */
                resetZoom(): void {
                    patchState(store, {
                        zoom: {
                            ...store.zoom(),
                            zoomLevel: 1
                        }
                    });
                },

                /**
                 * Get formatted zoom label for display (e.g., "150%")
                 */
                zoomLabel(): string {
                    return `${Math.round(store.zoom().zoomLevel * 100)}%`;
                },

                /**
                 * Set zoom level directly (clamped between 0.1 and 3.0)
                 */
                setZoomLevel(zoomLevel: number): void {
                    const clampedZoom = Math.max(0.1, Math.min(3, zoomLevel));
                    patchState(store, {
                        zoom: {
                            ...store.zoom(),
                            zoomLevel: clampedZoom
                        }
                    });
                },

                /**
                 * Set zoom mode state (active during zoom gestures)
                 */
                setZoomMode(isZoomMode: boolean): void {
                    patchState(store, {
                        zoom: {
                            ...store.zoom(),
                            isZoomMode
                        }
                    });
                },

                /**
                 * Set iframe document height for canvas calculations
                 */
                setIframeDocHeight(height: number): void {
                    patchState(store, {
                        zoom: {
                            ...store.zoom(),
                            iframeDocHeight: height
                        }
                    });
                },

                /**
                 * Set gesture start zoom level (for trackpad pinch gestures)
                 */
                setGestureStartZoom(zoom: number): void {
                    patchState(store, {
                        zoom: {
                            ...store.zoom(),
                            gestureStartZoom: zoom
                        }
                    });
                },

                /**
                 * Get current gesture start zoom level
                 */
                getGestureStartZoom(): number {
                    return store.zoom().gestureStartZoom;
                }
            };
        })
    );
}
