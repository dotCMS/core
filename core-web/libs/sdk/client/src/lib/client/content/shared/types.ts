import { Contentlet, DotHttpError } from '@dotcms/types';

import { Equals } from '../builders/query/lucene-syntax';
import { QueryBuilder } from '../builders/query/query';

/**
 * Content API specific error class
 * Wraps HTTP errors and adds content-specific context including query information
 */
export class DotErrorContent extends Error {
    public readonly httpError?: DotHttpError;
    public readonly contentType: string;
    public readonly operation: string;
    public readonly query?: string;

    constructor(message: string, contentType: string, operation: string, httpError?: DotHttpError, query?: string) {
        super(message);
        this.name = 'DotCMSContentError';
        this.contentType = contentType;
        this.operation = operation;
        this.httpError = httpError;
        this.query = query;

        // Ensure proper prototype chain for instanceof checks
        Object.setPrototypeOf(this, DotErrorContent.prototype);
    }

    /**
     * Serializes the error to a plain object for logging or transmission
     */
    toJSON() {
        return {
            name: this.name,
            message: this.message,
            contentType: this.contentType,
            operation: this.operation,
            httpError: this.httpError?.toJSON(),
            query: this.query,
            stack: this.stack
        };
    }
}

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
 * Callback for a fulfilled promise.
 *
 * @template T - The type of the response.
 * @callback OnFullfilled
 * @param {GetCollectionResponse<T>} value - The response value.
 * @returns {GetCollectionResponse<T> | PromiseLike<GetCollectionResponse<T>>} The processed response or a promise.
 */
export type OnFullfilled<T> =
    | ((
          value: GetCollectionResponse<T>
      ) => GetCollectionResponse<T> | PromiseLike<GetCollectionResponse<T>>)
    | undefined
    | null;

/**
 * Callback for a rejected promise.
 *
 * @callback OnRejected
 * @param {DotErrorContent} error - The content error object.
 * @returns {DotErrorContent | PromiseLike<DotErrorContent>} The processed error or a promise.
 */
export type OnRejected =
    | ((error: DotErrorContent) => DotErrorContent | PromiseLike<DotErrorContent>)
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
