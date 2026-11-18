/*
 * Constants of the click tracker.
 */

/**
 * Event type for content clicks
 * Must match DotCMSPredefinedEventType.CONTENT_CLICK
 */
export const CLICK_EVENT_TYPE = 'content_click';

/**
 * Default debounce time in milliseconds for clicks
 */
export const DEFAULT_CLICK_THROTTLE_MS = 300;

/**
 * CSS selector for clickable elements to track
 * Only clicks on <a> and <button> elements are tracked
 */
export const CLICKABLE_ELEMENTS_SELECTOR = 'a, button';
