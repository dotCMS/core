/*
 * Constants of the impression tracker.
 */

/**
 * Impression tracking configuration constants
 */

/**
 * Default minimum percentage of element that must be visible (0.0 to 1.0)
 */
export const DEFAULT_IMPRESSION_VISIBILITY_THRESHOLD = 0.5;

/**
 * Default minimum time in milliseconds element must be visible
 */
export const DEFAULT_IMPRESSION_DWELL_MS = 750;

/**
 * Default maximum number of elements to track (performance limit)
 */
export const DEFAULT_IMPRESSION_MAX_NODES = 100;

/**
 * Default throttle time in milliseconds for intersection callbacks
 */
export const DEFAULT_IMPRESSION_THROTTLE_MS = 100;

/**
 * Default impression tracking configuration
 */
export const DEFAULT_IMPRESSION_CONFIG = {
    visibilityThreshold: DEFAULT_IMPRESSION_VISIBILITY_THRESHOLD,
    dwellMs: DEFAULT_IMPRESSION_DWELL_MS,
    maxNodes: DEFAULT_IMPRESSION_MAX_NODES,
    throttleMs: DEFAULT_IMPRESSION_THROTTLE_MS
} as const;

/**
 * Event type for content impressions
 * Must match DotCMSPredefinedEventType.CONTENT_IMPRESSION
 */
export const IMPRESSION_EVENT_TYPE = 'content_impression';
