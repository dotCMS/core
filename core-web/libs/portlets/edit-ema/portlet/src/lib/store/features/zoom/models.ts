export interface ViewZoomCanvasStyles {
    outer: {
        width: string;
        height: string;
    };
    inner: {
        width: string;
        height: string;
        transform: string;
        transformOrigin: string;
    };
}

/**
 * View Zoom UI State (transient)
 * Manages zoom level, zoom mode, and canvas dimensions for the editor
 */
export interface ViewZoomState {
    /** Current zoom level (0.1 to 3.0) */
    viewZoomLevel: number;
    /** Whether zoom mode is active (temporary state during zoom gestures) */
    viewZoomIsActive: boolean;
    /** Height of the iframe document for canvas calculations */
    viewIframeDocHeight: number;
    /** Zoom level at gesture start (for trackpad pinch gestures) */
    viewGestureStartZoom: number;
}
