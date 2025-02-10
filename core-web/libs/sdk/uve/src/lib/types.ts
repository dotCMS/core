/**
 * Represents the state of the Universal Visual Editor (UVE)
 * @interface
 * @property {UVE_MODE} mode - The current mode of operation for UVE (EDIT, PREVIEW, LIVE, or UNKNOWN)
 * @property {string | null} persona - The selected persona for content personalization
 * @property {string | null} variantName - The name of the current content variant
 * @property {string | null} experimentId - The identifier for the current A/B testing experiment
 * @property {string | null} publishDate - The scheduled publish date for content
 * @property {string | null} languageId - The identifier for the current language selection
 */
export interface UVEState {
    mode: UVE_MODE;
    persona: string | null;
    variantName: string | null;
    experimentId: string | null;
    publishDate: string | null;
    languageId: string | null;
}

/**
 * Possible modes of UVE (Universal Visual Editor)
 * @enum {string}
 *
 * @property {string} LIVE - Shows published and future content
 * @property {string} PREVIEW - Shows published and working content
 * @property {string} EDIT - Enables content editing functionality in UVE
 * @property {string} UNKNOWN - Error state, UVE should not remain in this mode
 */
export enum UVE_MODE {
    EDIT = 'EDIT_MODE',
    PREVIEW = 'PREVIEW_MODE',
    LIVE = 'LIVE',
    UNKNOWN = 'UNKNOWN'
}
