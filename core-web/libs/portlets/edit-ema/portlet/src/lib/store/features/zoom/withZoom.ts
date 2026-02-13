import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withMethods,
    withState
} from '@ngrx/signals';

import { computed } from '@angular/core';

import { ViewZoomCanvasStyles, ViewZoomState } from './models';

import { UVEState } from '../../models';

const initialViewZoomState: ViewZoomState = {
    viewZoomLevel: 1,
    viewZoomIsActive: false,
    viewIframeDocHeight: 0,
    viewGestureStartZoom: 1
};

/**
 * View Zoom feature for the UVE store
 * Manages zoom level, zoom mode, and canvas dimensions for the editor
 *
 * @export
 * @return {*}
 */
export function withViewZoom() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withState<{ viewZoom: ViewZoomState }>({
            viewZoom: initialViewZoomState
        }),
        withComputed((store) => ({
            $viewCanvasOuterStyles: computed<ViewZoomCanvasStyles['outer']>(() => {
                const zoom = store.viewZoom().viewZoomLevel;
                const height = store.viewZoom().viewIframeDocHeight || 800;
                return {
                    width: `${1520 * zoom}px`,
                    height: `${height * zoom}px`
                };
            }),
            $viewCanvasInnerStyles: computed<ViewZoomCanvasStyles['inner']>(() => {
                const zoom = store.viewZoom().viewZoomLevel;
                const height = store.viewZoom().viewIframeDocHeight || 800;
                return {
                    width: `1520px`,
                    height: `${height}px`,
                    transform: `scale(${zoom})`,
                    transformOrigin: 'top left'
                };
            }),
            $viewZoomLevel: computed<number>(() => store.viewZoom().viewZoomLevel),
            $viewZoomIsActive: computed<boolean>(() => store.viewZoom().viewZoomIsActive),
            $viewIframeDocHeight: computed<number>(() => store.viewZoom().viewIframeDocHeight)
        })),
        withMethods((store) => {
            return {
                /**
                 * Increase zoom level by 0.1 (max 3.0)
                 */
                viewZoomIn(): void {
                    const currentZoom = store.viewZoom().viewZoomLevel;
                    const newZoom = Math.max(0.1, Math.min(3, currentZoom + 0.1));
                    patchState(store, {
                        viewZoom: {
                            ...store.viewZoom(),
                            viewZoomLevel: newZoom
                        }
                    });
                },

                /**
                 * Decrease zoom level by 0.1 (min 0.1)
                 */
                viewZoomOut(): void {
                    const currentZoom = store.viewZoom().viewZoomLevel;
                    const newZoom = Math.max(0.1, Math.min(3, currentZoom - 0.1));
                    patchState(store, {
                        viewZoom: {
                            ...store.viewZoom(),
                            viewZoomLevel: newZoom
                        }
                    });
                },

                /**
                 * Reset zoom level to 1.0 (100%)
                 */
                viewZoomReset(): void {
                    patchState(store, {
                        viewZoom: {
                            ...store.viewZoom(),
                            viewZoomLevel: 1
                        }
                    });
                },

                /**
                 * Get formatted zoom label for display (e.g., "150%")
                 */
                viewZoomLabel(): string {
                    return `${Math.round(store.viewZoom().viewZoomLevel * 100)}%`;
                },

                /**
                 * Set zoom level directly (clamped between 0.1 and 3.0)
                 */
                viewZoomSetLevel(viewZoomLevel: number): void {
                    const clampedZoom = Math.max(0.1, Math.min(3, viewZoomLevel));
                    patchState(store, {
                        viewZoom: {
                            ...store.viewZoom(),
                            viewZoomLevel: clampedZoom
                        }
                    });
                },

                /**
                 * Set zoom mode state (active during zoom gestures)
                 */
                viewZoomSetActive(viewZoomIsActive: boolean): void {
                    patchState(store, {
                        viewZoom: {
                            ...store.viewZoom(),
                            viewZoomIsActive
                        }
                    });
                },

                /**
                 * Set iframe document height for canvas calculations
                 */
                viewSetIframeDocHeight(height: number): void {
                    patchState(store, {
                        viewZoom: {
                            ...store.viewZoom(),
                            viewIframeDocHeight: height
                        }
                    });
                },

                /**
                 * Set gesture start zoom level (for trackpad pinch gestures)
                 */
                viewZoomSetGestureStart(zoom: number): void {
                    patchState(store, {
                        viewZoom: {
                            ...store.viewZoom(),
                            viewGestureStartZoom: zoom
                        }
                    });
                },

                /**
                 * Get current gesture start zoom level
                 */
                viewZoomGetGestureStart(): number {
                    return store.viewZoom().viewGestureStartZoom;
                }
            };
        })
    );
}
