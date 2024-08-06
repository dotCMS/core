/**
 * @description Configuration for fetching data using GraphQL.
 * @typedef {Object} GraphQLFetchConfig
 * @property {"GRAPHQL"} type - The type of fetch to perform. It can be either 'GRAPHQL' or 'PAGEAPI'.
 * @property {string} data - The query to be sent to the GRAPHQL endpoint.
 * @example
 * {
 *   type: 'GRAPHQL',
 *   data: 'query { ... }'
 * }
 */
export type GraphQLFetchConfig = {
    type: 'GRAPHQL';
    data: string;
};

/**
 * @description Configuration for fetching data using Page API.
 * @typedef {Object} PageAPIFetchConfig
 * @property {"PAGEAPI"} type - The type of fetch to perform. It can be either 'GRAPHQL' or 'PAGEAPI'.
 * @property {{ [key: string]: string }} data - Parameters to be sent to the PAGEAPI endpoint.
 * @example
 * {
 *   type: 'PAGEAPI',
 *   data: { depth: '2' }
 * }
 */
export type PageAPIFetchConfig = {
    type: 'PAGEAPI';
    data: { [key: string]: string };
};

/**
 * @description Union type for fetch configurations.
 * @typedef {GraphQLFetchConfig | PageAPIFetchConfig} DotCMSFetchConfig
 */
export type DotCMSFetchConfig = GraphQLFetchConfig | PageAPIFetchConfig;

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
     * The query string to use when fetching the page data.
     * @type {string}
     * @memberof DotCMSPageEditorConfig
     */
    query?: string;
    /**
     * The fetch configuration for the page data.
     * @type {DotCMSFetchConfig}
     * @memberof DotCMSPageEditorConfig
     * @description The fetch configuration can be either a GraphQLFetchConfig or a PageAPIFetchConfig.
     * @example
     * {
     *  type: 'GRAPHQL',
     *  data: 'query { ... }'
     * }
     * @example
     * {
     * type: 'PAGEAPI',
     * data: { depth: '2' }
     * }
     */
    fetch?: DotCMSFetchConfig;
    /**
     * The reload function to call when the page is reloaded.
     * @deprecated In future implementation we will be listening for the changes from the editor to update the page state so reload will not be needed.
     * @type {Function}
     */
    onReload?: () => void;
}
