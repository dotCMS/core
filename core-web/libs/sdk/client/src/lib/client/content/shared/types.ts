import { Equals } from '../../../query-builder/lucene-syntax';
import { QueryBuilder } from '../../../query-builder/sdk-query-builder';

/**
 * Model to sort by fields.
 */
export type SortBy = {
    /**
     * The field to sort by.
     */
    field: string;
    /**
     * The order of sorting: 'asc' for ascending, 'desc' for descending.
     */
    order: 'asc' | 'desc';
};

/**
 * Callback to build a query.
 *
 * @callback BuildQuery
 * @param {QueryBuilder} qb - The query builder instance.
 * @returns {Equals} The built query.
 */
export type BuildQuery = (qb: QueryBuilder) => Equals;

/**
 * Main fields of a Contentlet (Inherited from the Content Type).
 */
export interface ContentTypeMainFields {
    hostName: string;
    modDate: string;
    publishDate: string;
    title: string;
    baseType: string;
    inode: string;
    archived: boolean;
    ownerName: string;
    host: string;
    working: boolean;
    locked: boolean;
    stInode: string;
    contentType: string;
    live: boolean;
    owner: string;
    identifier: string;
    publishUserName: string;
    publishUser: string;
    languageId: number;
    creationDate: string;
    url: string;
    titleImage: string;
    modUserName: string;
    hasLiveVersion: boolean;
    folder: string;
    hasTitleImage: boolean;
    sortOrder: number;
    modUser: string;
    __icon__: string;
    contentTypeIcon: string;
    variant: string;
}

/**
 * The contentlet has the main fields and the custom fields of the content type.
 *
 * @template T - The custom fields of the content type.
 */
export type Contentlet<T> = T & ContentTypeMainFields;

/**
 * Callback for a fulfilled promise.
 *
 * @template T - The type of the response.
 * @callback OnFullfilled
 * @param {GetCollectionResponse<T>} value - The response value.
 * @returns {GetCollectionResponse<T> | PromiseLike<GetCollectionResponse<T>> | void} The processed response or a promise.
 */
export type OnFullfilled<T> =
    | ((
          value: GetCollectionResponse<T>
      ) => GetCollectionResponse<T> | PromiseLike<GetCollectionResponse<T>> | void)
    | undefined
    | null;

/**
 * Callback for a rejected promise.
 *
 * @callback OnRejected
 * @param {GetCollectionError} error - The error object.
 * @returns {GetCollectionError | PromiseLike<GetCollectionError> | void} The processed error or a promise.
 */
export type OnRejected =
    | ((error: GetCollectionError) => GetCollectionError | PromiseLike<GetCollectionError> | void)
    | undefined
    | null;

/**
 * Response of the get collection method.
 *
 * @template T - The type of the contentlet.
 */
export interface GetCollectionResponse<T> {
    /**
     * The list of contentlets.
     */
    contentlets: Contentlet<T>[];
    /**
     * The current page number.
     */
    page: number;
    /**
     * The size of the page.
     */
    size: number;
    /**
     * The total number of contentlets.
     */
    total: number;
    /**
     * The fields by which the contentlets are sorted.
     */
    sortedBy?: SortBy[];
}

/**
 * Raw response of the get collection method.
 *
 * @template T - The type of the contentlet.
 */
export interface GetCollectionRawResponse<T> {
    entity: {
        jsonObjectView: {
            /**
             * The list of contentlets.
             */
            contentlets: Contentlet<T>[];
        };
        /**
         * The size of the results.
         */
        resultsSize: number;
    };
}

/**
 * Error object for the get collection method.
 */
export interface GetCollectionError {
    /**
     * The status code of the error.
     */
    status: number;
    [key: string]: unknown;
}
