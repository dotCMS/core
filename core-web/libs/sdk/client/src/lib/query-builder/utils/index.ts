import { Field } from './lucene-syntax/Field';
import { NotOperand } from './lucene-syntax/NotOperand';
import { Operand } from './lucene-syntax/Operand';
import { Term } from './lucene-syntax/Term';

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
 * @export
 * @param {string} term
 * @return {*}  {string}
 */
export function sanitizePhrases(term: string): string {
    return term.includes(' ') ? `"${term}"` : term;
}

/**
 * This function builds a term to be used in a lucene query.
 * We need to sanitize the term before adding it to the query.
 *
 * @export
 * @param {string} query
 * @param {string} term
 * @return {*}  {Term}
 */
export function buildTerm(query: string, term: string): Term {
    const newQuery = query + sanitizePhrases(term);

    return new Term(newQuery);
}

/**
 * This function builds a term to be used in a lucene query.
 * We need to sanitize the raw query before adding it to the query.
 *
 * @export
 * @param {string} query
 * @param {string} raw
 * @return {*}  {Term}
 */
export function buildRawTerm(query: string, raw: string): Term {
    const newQuery = query + ` ${raw}`;

    return new Term(sanitizeQuery(newQuery));
}

/**
 * This function builds a field to be used in a lucene query.
 * We need to format the field before adding it to the query.
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
 * @export
 * @param {string} query
 * @return {*}  {NotOperand}
 */
export function buildNotOperand(query: string): NotOperand {
    const newQuery = query + ` ${OPERAND.NOT} `;

    return new NotOperand(newQuery);
}
