import { Field } from './Field';
import { NotOperand } from './NotOperand';
import { Operand } from './Operand';

import {
    OPERAND,
    buildExcludeField,
    buildField,
    buildNotOperand,
    buildOperand,
    buildRawEquals,
    sanitizeQuery
} from '../utils';

/**
 * 'Equal' Is a Typescript class that provides the ability to use terms in the lucene query string.
 * A term is a value used to search for a specific value in a document. It can be a word or a phrase.
 *
 * Ex: myValue or "My Value"
 *
 * @export
 * @class Equal
 */
export class Equals {
    #query = '';

    constructor(private query: string) {
        this.#query = this.query;
    }

    /**
     * This method appends to the query a term that should be excluded in the search.
     *
     * @example
     * ```ts
     * const equals = new Equals("+myField: myValue");
     * equals.excludeField("myField2").equals("myValue2"); // Current query: "+myField: myValue -myField2: myValue2"
     * ```
     *
     * @param {string} field - The field that should be excluded in the search.
     * @return {*}  {Field} - An instance of a Lucene Field. A field is a key used to search for a specific value in a document.
     * @memberof Equal
     */
    excludeField(field: string): Field {
        return buildExcludeField(this.#query, field);
    }

    /**
     * This method appends to the query a field that should be included in the search.
     *
     * @example
     * ```ts
     * const equals = new Equals("+myField: myValue");
     * equals.field("myField2").equals("myValue2");  // Current query: "+myField: myValue +myField2: myValue2"
     *```
     * @param {string} field - The field that should be included in the search.
     * @return {*}  {Field} - An instance of a Lucene Field. A field is a key used to search for a specific value in a document.
     * @memberof Equal
     */
    field(field: string): Field {
        return buildField(this.#query, field);
    }

    /**
     * This method appends to the query an operand to use logic operators in the query.
     *
     * @example
     * @example
     * ```ts
     * const equals = new Equals("+myField: myValue");
     * equals.or().field("myField2").equals("myValue2"); // Current query: "+myField: myValue OR +myField2: myValue2"
     * ```
     *
     * @return {*}  {Operand} - An instance of a Lucene Operand. An operand is a logical operator used to combine terms or phrases in a query.
     * @memberof Equal
     */
    or(): Operand {
        return buildOperand(this.#query, OPERAND.OR);
    }

    /**
     * This method appends to the query an operand to use logic operators in the query.
     *
     * @example
     * ```ts
     * const equals = new Equals("+myField: myValue");
     * equals.and().field("myField2").equals("myValue2"); // Current query: "+myField: myValue AND +myField2: myValue2"
     * ```
     *
     * @return {*}  {Operand} - An instance of a Lucene Operand. An operand is a logical operator used to combine terms or phrases in a query.
     * @memberof Equal
     */
    and(): Operand {
        return buildOperand(this.#query, OPERAND.AND);
    }

    /**
     * This method appends to the query an operand to use logic operators in the query.
     *
     * @example
     * ```ts
     * const equals = new Equals("+myField: myValue");
     * equals.not().field("myField").equals("myValue2"); // Current query: "+myField: myValue NOT +myField: myValue2"
     * ```
     *
     * @return {*}  {NotOperand} - An instance of a Lucene Not Operand. A not operand is a logical operator used to exclude terms or phrases in a query.
     * @memberof Equal
     */
    not(): NotOperand {
        return buildNotOperand(this.#query);
    }

    /**
     * This method allows to pass a raw query string to the query builder.
     * This raw query should end in a Lucene Equal.
     * This method is useful when you want to append a complex query or an already written query to the query builder.
     *
     * @example
     * ```ts
     * // This builds the follow raw query "+myField: value AND (someOtherValue OR anotherValue)"
     * const equals = new Equals("+myField: value");
     * equals.raw("+myField2: value2"); // Current query: "+myField: value +myField2: anotherValue"
     * ```
     *
     * @param {string} query - A raw query string.
     * @return {*}  {Equal} - An instance of a Lucene Equal. A term is a value used to search for a specific value in a document.
     * @memberof QueryBuilder
     */
    raw(query: string): Equals {
        return buildRawEquals(this.#query, query);
    }

    /**
     * This method returns the final query string.
     *
     * @example
     * ```ts
     * const equals = new Equals("+myField: myValue");
     * equals.field("myField2").equals("myValue2").build(); // Returns "+myField: myValue +myField2: myValue2"
     * ```
     *
     * @return {*}  {string} - The final query string.
     * @memberof Equal
     */
    build(): string {
        return sanitizeQuery(this.#query);
    }
}
