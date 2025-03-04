import { Equals } from './Equals';

import { buildEquals } from '../utils';

/**
 * The `Field` class is used to build a query with a specific field.
 * A Lucene Field is a key used to search for a specific value in a document.
 *
 * @export
 * @class Field
 */
export class Field {
    #query = '';

    /**
     * Creates an instance of the `Field` class.
     *
     * @param {string} query - The initial query string.
     */
    constructor(private query: string) {
        this.#query = this.query;
    }

    /**
     * Appends a term to the query that should be included in the search.
     *
     * @example
     * ```typescript
     * const field = new Field("+myField");
     * field.equals("myValue");
     * ```
     *
     * @param {string} term - The term that should be included in the search.
     * @return {Equals} - An instance of `Equals`.
     * @memberof Field
     */
    equals(term: string): Equals {
        return buildEquals(this.#query, term);
    }
}
