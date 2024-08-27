/**
 * @description Custom client parameters for fetching data.
 */
export type CustomClientParams = {
    depth: string;
};

/**
 * @description Union type for fetch configurations.
 * @typedef {GraphQLFetchConfig | PageAPIFetchConfig} DotCMSFetchConfig
 */
export type EditorConfig =
    | {
          params: CustomClientParams;
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
    editor?: EditorConfig;
    /**
     * The reload function to call when the page is reloaded.
     * @deprecated In future implementation we will be listening for the changes from the editor to update the page state so reload will not be needed.
     * @type {Function}
     */
    onReload?: () => void;
}
