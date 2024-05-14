import { Term } from './Term';

import { buildTerm } from '..';

/**
 * 'Field' class is used to build a query with a field.
 * A Lucene Field is a key used to search for a specific value in a document.
 *
 * @export
 * @class Field
 */
export class Field {
    #query = '';
    constructor(private query: string) {
        this.#query = this.query;
    }

    /**
     * This method appends to the query a term that should be included in the search..
     *
     * Ex: myValue or "My value"
     *
     * @param {string} term - The term that should be included in the search.
     * @return {*}  {Term} - An instance of a Lucene Term.
     * @memberof Field
     */
    term(term: string): Term {
        return buildTerm(this.#query, term);
    }
}
