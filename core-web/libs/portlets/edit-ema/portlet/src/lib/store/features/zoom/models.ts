export interface ZoomCanvasStyles {
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
 * Zoom UI State (transient)
 * Manages zoom level, zoom mode, and canvas dimensions for the editor
 */
export interface ZoomState {
    /** Current zoom level (0.1 to 3.0) */
    zoomLevel: number;
    /** Whether zoom mode is active (temporary state during zoom gestures) */
    isZoomMode: boolean;
    /** Height of the iframe document for canvas calculations */
    iframeDocHeight: number;
    /** Zoom level at gesture start (for trackpad pinch gestures) */
    gestureStartZoom: number;
}
