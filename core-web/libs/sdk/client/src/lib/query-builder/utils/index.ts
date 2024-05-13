import { Field } from './lucene-syntax/Field';
import { NotOperand } from './lucene-syntax/NotOperand';
import { Operand } from './lucene-syntax/Operand';
import { Term } from './lucene-syntax/Term';

export enum OPERAND {
    OR = 'OR',
    AND = 'AND',
    NOT = 'NOT'
}

export function santizeQuery(str: string): string {
    return str.replace(/\s{2,}/g, ' ').trim();
}

export function sanitizePhrases(term: string): string {
    return term.includes(' ') ? `"${term}"` : term;
}

export function buildTerm(query: string, term: string): Term {
    const newQuery = query + sanitizePhrases(term);

    return new Term(newQuery);
}

export function buildRawTerm(query: string, raw: string): Term {
    const newQuery = query + ` ${raw}`;

    return new Term(santizeQuery(newQuery));
}

export function buildField(query: string, field: string, exclude = false): Field {
    const newQuery = query + (exclude ? ` -${field}:` : ` +${field}:`);

    return new Field(newQuery);
}

export function buildOperand(query: string, operand: OPERAND): Operand {
    const newQuery = query + ` ${operand} `;

    return new Operand(newQuery);
}

export function buildNotOperand(query: string): NotOperand {
    const newQuery = query + ` ${OPERAND.NOT} `;

    return new NotOperand(newQuery);
}
