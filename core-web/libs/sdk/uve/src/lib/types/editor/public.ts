import { DotCMSClientParams } from './editor.model';

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

/**
 * Callback function for UVE events
 * @callback UVECallback
 * @param {unknown} payload - The payload of the event
 */
export type UVECallback = (payload: unknown) => void;

/**
 * Unsubscribe function for UVE events
 * @callback UnsubscribeUVE
 */
export type UnsubscribeUVE = () => void;

/**
 * UVESubscription type
 * @typedef {Object} UVESubscription
 * @property {UnsubscribeUVE} unsubscribe - The unsubscribe function for the UVE event
 * @property {string} event - The event name
 */
export type UVESubscription = {
    unsubscribe: UnsubscribeUVE;
    event: string;
};

/**
 * UVE event type
 * @typedef {function} UVEEvent
 */
export type UVEEvent = (callback: UVECallback) => UVESubscription;

/**
 * @description Union type for fetch configurations.
 * @typedef {GraphQLFetchConfig | PageAPIFetchConfig} DotCMSFetchConfig
 */
export type DotCMSEditorConfig =
    | {
          params: DotCMSClientParams;
      }
    | {
          query: string;
      };

/**
 * Represents the configuration options for the DotCMS page editor.
 * @export
 * @interface DotCMSPageEditorConfig
 */
export interface DotCMSPageEditorConfig {
    /**
     * The pathname of the page being edited. Optional.
     * @type {string}
     */
    pathname: string;
    /**
     *
     * @type {DotCMSFetchConfig}
     * @memberof DotCMSPageEditorConfig
     * @description The configuration custom params for data fetching on Edit Mode.
     * @example <caption>Example with Custom GraphQL query</caption>
     * const config: DotCMSPageEditorConfig = {
     *   editor: { query: 'query { ... }' }
     * };
     *
     * @example <caption>Example usage with Custom Page API parameters</caption>
     * const config: DotCMSPageEditorConfig = {
     *   editor: { params: { depth: '2' } }
     * };
     */
    editor?: DotCMSEditorConfig;
    /**
     * The reload function to call when the page is reloaded.
     * @deprecated In future implementation we will be listening for the changes from the editor to update the page state so reload will not be needed.
     * @type {Function}
     */
    onReload?: () => void;
}
