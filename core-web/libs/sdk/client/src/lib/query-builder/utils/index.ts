import { Equals } from '../lucene-syntax/Equals';
import { Field } from '../lucene-syntax/Field';
import { NotOperand } from '../lucene-syntax/NotOperand';
import { Operand } from '../lucene-syntax/Operand';

/**
 * Enum for common Operands
 *
 * @export
 * @enum {number}
 */
export enum OPERAND {
    OR = 'OR',
    AND = 'AND',
    NOT = 'NOT'
}

/**
 * This function removes extra spaces from a string.
 *
 * @example
 * ```ts
 * sanitizeQuery("  my query  "); // Output: "my query"
 * ```
 *
 * @export
 * @param {string} str
 * @return {*}  {string}
 */
export function sanitizeQuery(str: string): string {
    return str.replace(/\s{2,}/g, ' ').trim();
}

/**
 * This function sanitizes a term by adding quotes if it contains spaces.
 * In lucene, a term with spaces should be enclosed in quotes.
 *
 * @example
 * ```ts
 * sanitizePhrases(`my term`); // Output: `"my term"`
 * sanitizePhrases(`myterm`); // Output: `myterm`
 * ```
 *
 * @export
 * @param {string} term
 * @return {*}  {string}
 */
export function sanitizePhrases(term: string): string {
    return term.includes(' ') ? `'${term}'` : term;
}

/**
 * This function builds a term to be used in a lucene query.
 * We need to sanitize the term before adding it to the query.
 *
 * @example
 * ```ts
 * const equals = buildEquals("+myField: ", "myValue"); // Current query: "+myField: myValue"
 * ```
 *
 * @export
 * @param {string} query
 * @param {string} term
 * @return {*}  {Equals}
 */
export function buildEquals(query: string, term: string): Equals {
    const newQuery = query + sanitizePhrases(term);

    return new Equals(newQuery);
}

/**
 * This function builds a term to be used in a lucene query.
 * We need to sanitize the raw query before adding it to the query.
 *
 * @example
 * ```ts
 * const query = "+myField: myValue";
 * const field = buildRawEquals(query, "-myField2: myValue2"); // Current query: "+myField: myValue -myField2: myValue"
 * ```
 *
 * @export
 * @param {string} query
 * @param {string} raw
 * @return {*}  {Equals}
 */
export function buildRawEquals(query: string, raw: string): Equals {
    const newQuery = query + ` ${raw}`;

    return new Equals(sanitizeQuery(newQuery));
}

/**
 * This function builds a field to be used in a lucene query.
 * We need to format the field before adding it to the query.
 *
 * @example
 * ```ts
 * const field = buildField("+myField: ", "myValue"); // Current query: "+myField: myValue"
 * ```
 *
 * @export
 * @param {string} query
 * @param {string} field
 * @return {*}  {Field}
 */
export function buildField(query: string, field: string): Field {
    const newQuery = query + ` +${field}:`;

    return new Field(newQuery);
}

/**
 * This function builds an exclude field to be used in a lucene query.
 * We need to format the field before adding it to the query.
 *
 * @example
 * ```ts
 * const query = "+myField: myValue";
 * const field = buildExcludeField(query, "myField2"); // Current query: "+myField: myValue -myField2:"
 * ```
 *
 * @export
 * @param {string} query
 * @param {string} field
 * @return {*}  {Field}
 */
export function buildExcludeField(query: string, field: string): Field {
    const newQuery = query + ` -${field}:`;

    return new Field(newQuery);
}

/**
 * This function builds an operand to be used in a lucene query.
 * We need to format the operand before adding it to the query.
 *
 * @example
 * <caption>E.g. Using the AND operand</caption>
 * ```ts
 * const query = "+myField: myValue";
 * const field = buildOperand(query, OPERAND.AND); // Current query: "+myField: myValue AND"
 * ```
 * @example
 * <caption>E.g. Using the OR operand</caption>
 * ```ts
 * const query = "+myField: myValue";
 * const field = buildOperand(query, OPERAND.OR); // Current query: "+myField: myValue OR"
 * ```
 * @export
 * @param {string} query
 * @param {OPERAND} operand
 * @return {*}  {Operand}
 */
export function buildOperand(query: string, operand: OPERAND): Operand {
    const newQuery = query + ` ${operand} `;

    return new Operand(newQuery);
}

/**
 * This function builds a NOT operand to be used in a lucene query.
 * We need to format the operand before adding it to the query.
 *
 * @example
 * ```ts
 * const query = "+myField: myValue";
 * const field = buildNotOperand(query); // Current query: "+myField: myValue NOT"
 * ```
 *
 * @export
 * @param {string} query
 * @return {*}  {NotOperand}
 */
export function buildNotOperand(query: string): NotOperand {
    const newQuery = query + ` ${OPERAND.NOT} `;

    return new NotOperand(newQuery);
}
