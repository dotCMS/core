import { Equals } from './Equals';

import { buildEquals } from '../utils';

/**
 * 'NotOperand' Is a Typescript class that provides the ability to use the NOT operand in the lucene query string.
 *
 * @export
 * @class NotOperand
 */
export class NotOperand {
    #query = '';

    constructor(private query: string) {
        this.#query = this.query;
    }

    /**
     * This method appends to the query a term that should be included in the search.
     *
     * @example
     * ```typescript
     * const notOperand = new NotOperand("+myField");
     * notOperand.equals("myValue");
     * ```
     *
     * @param {string} term - The term that should be included in the search.
     * @return {*}  {Equals} - An instance of Equals.
     * @memberof NotOperand
     */
    equals(term: string): Equals {
        return buildEquals(this.#query, term);
    }
}
