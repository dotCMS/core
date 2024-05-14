import { buildExcludeField, buildField, buildRawTerm } from './utils';
import { Field } from './utils/lucene-syntax/Field';
import { Term } from './utils/lucene-syntax/Term';

/**
 * 'QueryBuilder' Is a Typescript class that provides the ability to build a query string using the Lucene syntax in a more readable way.
 *
 * @export
 * @class QueryBuilder
 */
export class QueryBuilder {
    #query = '';

    /**
     * This method appends to the query a field that should be included in the search.
     *
     * Ex: "+myField:"
     *
     * @param {string} field - The field that should be included in the search.
     * @return {*}  {Field} -  An instance of a Lucene Field. A field is a key used to search for a specific value in a document.
     * @memberof QueryBuilder
     */
    field(field: string): Field {
        return buildField(this.#query, field);
    }

    /**
     * This method appends to the query a field that should be excluded from the search.
     *
     * Ex: "-myField:"
     *
     * @param {string} field - The field that should be excluded from the search.
     * @return {*}  {Field} -  An instance of a Lucene Exclude Field. An exclude field is a key used to exclude for a specific value in a document.
     * @memberof QueryBuilder
     */
    excludeField(field: string): Field {
        return buildExcludeField(this.#query, field);
    }

    /**
     * This method allows to pass a raw query string to the query builder.
     * This raw query should end in a Lucene Term.
     * This method is useful when you want to append a complex query or an already written query to the query builder.
     *
     * Ex: "+myField: value AND (someOtherValue OR anotherValue)"
     *
     * @param {string} query - A raw query string.
     * @return {*}  {Term} - An instance of a Lucene Term. A term is a value used to search for a specific value in a document.
     * @memberof QueryBuilder
     */
    raw(query: string): Term {
        return buildRawTerm(this.#query, query);
    }
}
