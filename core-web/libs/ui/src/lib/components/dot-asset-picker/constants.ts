/** Toast lifetimes (ms) for the picker's upload feedback. */
export const SUCCESS_MESSAGE_LIFE = 4500;
export const WARNING_MESSAGE_LIFE = 4200;
export const ERROR_MESSAGE_LIFE = 4500;

/** Starting sidebar/content ratio — the same split the sidebar had as a fixed `w-1/4`. */
export const ASSET_PICKER_SPLITTER_SIZES = [25, 75];

/**
 * Per-panel minimums, in percent. PrimeNG's splitter has no `maxSize` in this version, so the
 * sidebar's ceiling is expressed as the content panel's floor: content never drops below 50%, which
 * caps the sidebar at 50%.
 */
export const ASSET_PICKER_SPLITTER_MIN_SIZES = [15, 50];
